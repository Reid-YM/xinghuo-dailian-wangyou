#!/usr/bin/env bash
# GitHub Actions 触发的生产发布入口。
# 该脚本必须在生产服务器上执行，并且只允许部署 GitHub main 的指定 SHA。
# 它会在服务器本机构建、18080 冒烟，然后复用 deploy-zero-downtime.sh 完成 8081 -> 8080 发布。
set -Eeuo pipefail

WORK=${WORK:-/srv/xinghuo-dev/work/xinghuo-v2}
APP_DIR=${APP_DIR:-/opt/xinghuo-v2}
CANDIDATE_DIR=${CANDIDATE_DIR:-$APP_DIR/candidates}
SMOKE_UNIT=${SMOKE_UNIT:-xinghuo-smoke-18080}
SMOKE_PORT=${SMOKE_PORT:-18080}
EXPECTED_SHA=${1:-}

log()  { echo "[$(date '+%F %T')] $*"; }
fail() { log "❌ $*"; exit 1; }

cleanup_smoke() {
  sudo systemctl stop "$SMOKE_UNIT" 2>/dev/null || true
  sudo systemctl reset-failed "$SMOKE_UNIT" 2>/dev/null || true
}
trap cleanup_smoke EXIT

[ "${CONFIRM_GITHUB_DEPLOY:-}" = "YES" ] \
  || fail "缺少 CONFIRM_GITHUB_DEPLOY=YES"

[[ "$EXPECTED_SHA" =~ ^[0-9a-f]{40}$ ]] \
  || fail "必须传入 40 位 Git commit SHA"

[ -d "$WORK/.git" ] || fail "正式工作区不是 Git 仓库: $WORK"
[ -f "$WORK/scripts/smoke_test.sh" ] || fail "缺少 smoke_test.sh"
[ -f "$WORK/scripts/deploy-zero-downtime.sh" ] || fail "缺少 deploy-zero-downtime.sh"

log "1/8 锁定 GitHub main"
CURRENT_BRANCH=$(git -C "$WORK" branch --show-current)
[ "$CURRENT_BRANCH" = "main" ] || fail "正式工作区不是 main: $CURRENT_BRANCH"
[ -z "$(git -C "$WORK" status --porcelain)" ] || fail "正式工作区不干净"

git -C "$WORK" fetch origin main
REMOTE_SHA=$(git -C "$WORK" rev-parse origin/main)
[ "$REMOTE_SHA" = "$EXPECTED_SHA" ] \
  || fail "GitHub origin/main 已不是目标 SHA: expected=$EXPECTED_SHA actual=$REMOTE_SHA"

git -C "$WORK" pull --ff-only origin main
HEAD_SHA=$(git -C "$WORK" rev-parse HEAD)
[ "$HEAD_SHA" = "$EXPECTED_SHA" ] \
  || fail "同步后的 HEAD 与目标 SHA 不一致"
[ -z "$(git -C "$WORK" status --porcelain)" ] || fail "同步后工作区不干净"

log "2/8 Flyway 自动发布安全阀"
MIGRATIONS=$(git -C "$WORK" diff --name-only \
  production-recovered-20261005..HEAD -- src/main/resources/db/migration)
if [ -n "$MIGRATIONS" ]; then
  echo "$MIGRATIONS"
  fail "检测到 Flyway migration 变化；一键发布默认拒绝数据库迁移版本，请走单独数据库发布评审"
fi

log "3/8 生产前置检查"
sudo systemctl is-active --quiet xinghuo-v2.service || fail "xinghuo-v2.service 不是 active"
sudo systemctl is-active --quiet nginx || fail "nginx 不是 active"
sudo nginx -t >/dev/null
curl -fsS --max-time 8 "http://127.0.0.1:8080/" >/dev/null || fail "部署前 HOME 健康检查失败"
curl -fsS --max-time 8 "http://127.0.0.1:8080/services" >/dev/null || fail "部署前 SERVICES 健康检查失败"
if sudo ss -lntp | grep -q ':8081\b'; then fail "8081 已被占用"; fi
if sudo ss -lntp | grep -q ':18080\b'; then fail "18080 已被占用"; fi
sudo grep -q '127\.0\.0\.1:8080;' /etc/nginx/conf.d/xinghuo-upstream.conf \
  || fail "nginx upstream 当前未指向 8080"

log "4/8 Maven 完整构建"
cd "$WORK"
mvn -B -ntp clean package
JAR="$WORK/target/xinghuo-v2-1.0.0.jar"
[ -f "$JAR" ] || fail "构建后未找到 JAR: $JAR"
BUILD_SHA=$(sha256sum "$JAR" | awk '{print $1}')
log "构建 JAR SHA256: $BUILD_SHA"

log "5/8 安装候选 JAR"
sudo install -d -o root -g xinghuo -m 0750 "$CANDIDATE_DIR"
CANDIDATE="$CANDIDATE_DIR/xinghuo-v2-$HEAD_SHA.jar"
sudo install -o xinghuo -g xinghuo -m 0640 "$JAR" "$CANDIDATE"
CANDIDATE_SHA=$(sudo -u xinghuo sha256sum "$CANDIDATE" | awk '{print $1}')
[ "$CANDIDATE_SHA" = "$BUILD_SHA" ] || fail "候选 JAR SHA256 不一致"
sudo -u xinghuo test -r "$CANDIDATE" || fail "候选 JAR 对运行用户不可读"

log "6/8 18080 预发布完整冒烟"
cleanup_smoke
sudo systemd-run \
  --unit="$SMOKE_UNIT" \
  --collect \
  --uid=xinghuo \
  --gid=xinghuo \
  --working-directory="$APP_DIR" \
  --property=EnvironmentFile="$APP_DIR/.env" \
  --property=Restart=no \
  /usr/bin/java -Xms192m -Xmx512m -jar "$CANDIDATE" --server.port="$SMOKE_PORT" >/dev/null

READY=0
for _ in $(seq 1 60); do
  if sudo ss -lntp | grep -q ":${SMOKE_PORT}\\b"; then
    READY=1
    break
  fi
  sleep 2
done

if [ "$READY" != "1" ]; then
  sudo journalctl -u "$SMOKE_UNIT" --no-pager -n 200 || true
  fail "$SMOKE_PORT 未开始监听"
fi

if ! bash "$WORK/scripts/smoke_test.sh" "$SMOKE_PORT"; then
  sudo journalctl -u "$SMOKE_UNIT" --no-pager -n 200 || true
  fail "$SMOKE_PORT 完整冒烟失败"
fi
log "✅ 18080 完整冒烟通过"
cleanup_smoke

mkdir -p "$WORK/build"
cat > "$WORK/build/predeploy-smoke.env" <<EOF
DEPLOY_GIT_SHA=$HEAD_SHA
CANDIDATE_JAR=$CANDIDATE
PREDEPLOY_SMOKE_SHA256=$CANDIDATE_SHA
SMOKE_PORT=$SMOKE_PORT
SMOKE_RESULT=PASS
SMOKE_AT=$(date '+%Y-%m-%d %H:%M:%S %z')
EOF

log "7/8 执行 8081 -> 8080 零停机正式发布"
# candidates 目录是 root:xinghuo 0750；正式发布脚本需要以 sudo 上下文读取同一个已冒烟候选 JAR。
sudo env \
  CONFIRM_PROD_DEPLOY=YES \
  PREDEPLOY_SMOKE_SHA256="$CANDIDATE_SHA" \
  DEPLOY_GIT_SHA="$HEAD_SHA" \
  bash "$WORK/scripts/deploy-zero-downtime.sh" "$CANDIDATE"

log "8/8 发布后核验"
sudo systemctl is-active --quiet xinghuo-v2.service || fail "发布后正式服务不是 active"
sudo systemctl is-active --quiet nginx || fail "发布后 nginx 不是 active"
curl -fsS --max-time 8 "http://127.0.0.1:8080/" >/dev/null || fail "发布后 HOME 健康检查失败"
curl -fsS --max-time 8 "http://127.0.0.1:8080/services" >/dev/null || fail "发布后 SERVICES 健康检查失败"
sudo grep -q '127\.0\.0\.1:8080;' /etc/nginx/conf.d/xinghuo-upstream.conf \
  || fail "发布后 nginx 未切回 8080"
if sudo ss -lntp | grep -q ':8081\b'; then fail "发布后 8081 仍在监听"; fi
if sudo ss -lntp | grep -q ':18080\b'; then fail "发布后 18080 仍在监听"; fi

CURRENT_JAR=$(readlink -f "$APP_DIR/current.jar")
CURRENT_SHA=$(sudo sha256sum "$CURRENT_JAR" | awk '{print $1}')
[ "$CURRENT_SHA" = "$CANDIDATE_SHA" ] || fail "发布后的 current.jar SHA256 与候选包不一致"

log "✅ GitHub 一键生产发布完成"
log "   Git SHA: $HEAD_SHA"
log "   JAR SHA256: $CANDIDATE_SHA"
log "   Current: $CURRENT_JAR"
log "   Nginx: 8080"
