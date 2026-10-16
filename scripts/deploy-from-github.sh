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
REVIEWED_MIGRATIONS_FILE=${REVIEWED_MIGRATIONS_FILE:-$WORK/docs/REVIEWED_FLYWAY_MIGRATIONS.txt}
RELEASE_ATTESTATION_FILE=${RELEASE_ATTESTATION_FILE:-$APP_DIR/release-attestation.json}
RELEASE_ATTESTATION_HISTORY_DIR=${RELEASE_ATTESTATION_HISTORY_DIR:-$APP_DIR/release-attestations}

log()  { echo "[$(date '+%F %T')] $*"; }
fail() { log "❌ $*"; exit 1; }

probe_http_millis() {
  local url=$1
  local seconds
  seconds=$(curl -fsS -o /dev/null --max-time 8 -w '%{time_total}' "$url") || return 1
  awk -v seconds="$seconds" 'BEGIN { printf "%d", (seconds * 1000) + 0.5 }'
}

cleanup_smoke() {
  sudo systemctl stop "$SMOKE_UNIT" 2>/dev/null || true
  sudo systemctl reset-failed "$SMOKE_UNIT" 2>/dev/null || true
}

smoke_port_busy() {
  sudo ss -lntp | grep -q ":${SMOKE_PORT}\\b"
}

prepare_smoke_port() {
  local i
  log "清理本项目已知预发布单元 $SMOKE_UNIT，并确认 :$SMOKE_PORT 可用"
  cleanup_smoke

  for i in $(seq 1 10); do
    if ! smoke_port_busy; then
      return 0
    fi
    sleep 1
  done

  log "⚠️ :$SMOKE_PORT 仍被占用；仅输出现场信息，不自动终止未知进程"
  sudo ss -lntp | grep ":${SMOKE_PORT}\\b" || true
  sudo systemctl status "$SMOKE_UNIT" --no-pager -n 40 || true
  return 1
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

# 判定口径：拿「本次要发布的包」和「线上正在运行的包」比 migration 清单，
# 而不是与固定的恢复标签 production-recovered-20261005 比较。
# 旧口径下，一旦有任何一个新 migration 进入 main，之后每次发布都会被拦下 —
# 哪怕那次改动完全不含数据库变更，把安全阀变成了永久路障。
LIVE_JAR=$(readlink -f "$APP_DIR/current.jar" 2>/dev/null || echo "$APP_DIR/current.jar")
LIVE_MIGRATIONS=""
if [ -f "$LIVE_JAR" ]; then
  LIVE_MIGRATIONS=$(sudo unzip -Z1 "$LIVE_JAR" 2>/dev/null \
    | sed -n 's#^BOOT-INF/classes/db/migration/\(V[^/]*\.sql\)$#\1#p' | sort -u) \
    || LIVE_MIGRATIONS=""
fi
HEAD_MIGRATIONS=$(find "$WORK/src/main/resources/db/migration" -maxdepth 1 -name 'V*.sql' \
  -printf '%f\n' 2>/dev/null | sort -u)

migration_has_exact_review() {
  local migration=$1
  local migration_path="$WORK/src/main/resources/db/migration/$migration"
  local actual_blob reviewed_blob
  [ -f "$migration_path" ] || return 1
  [ -f "$REVIEWED_MIGRATIONS_FILE" ] || return 1
  actual_blob=$(git -C "$WORK" hash-object "src/main/resources/db/migration/$migration")
  reviewed_blob=$(awk -v name="$migration" '$1 == name { print $2 }' "$REVIEWED_MIGRATIONS_FILE" | tail -n 1)
  [[ "$reviewed_blob" =~ ^[0-9a-f]{40}$ ]] || return 1
  [ "$actual_blob" = "$reviewed_blob" ]
}

if [ -z "$LIVE_MIGRATIONS" ]; then
  # 读不到线上包内的清单时退回旧口径：宁可拦下，也不能放过带迁移的版本
  MIGRATIONS=$(git -C "$WORK" diff --name-only \
    production-recovered-20261005..HEAD -- src/main/resources/db/migration)
  if [ -n "$MIGRATIONS" ]; then
    echo "$MIGRATIONS"
    fail "无法读取线上包内的 migration 清单，且与恢复标签存在差异；请人工核对后再发布"
  fi
else
  NEW_MIGRATIONS=$(comm -13 <(printf '%s\n' "$LIVE_MIGRATIONS") \
    <(printf '%s\n' "$HEAD_MIGRATIONS") | sed '/^$/d')
  if [ -n "$NEW_MIGRATIONS" ]; then
    echo "$NEW_MIGRATIONS"
    UNREVIEWED_MIGRATIONS=""
    while IFS= read -r migration; do
      [ -n "$migration" ] || continue
      if migration_has_exact_review "$migration"; then
        REVIEWED_BLOB=$(git -C "$WORK" hash-object "src/main/resources/db/migration/$migration")
        log "✅ 已评审 migration 内容匹配: $migration ($REVIEWED_BLOB)"
      else
        UNREVIEWED_MIGRATIONS="${UNREVIEWED_MIGRATIONS}${migration}"$'\n'
      fi
    done <<< "$NEW_MIGRATIONS"

    if [ -z "$UNREVIEWED_MIGRATIONS" ]; then
      log "⚠️ 本次新增 migration 均有精确内容评审记录，允许进入候选构建与冒烟"
    elif [ "${ALLOW_REVIEWED_MIGRATION:-}" = "YES" ]; then
      printf '%s' "$UNREVIEWED_MIGRATIONS"
      log "⚠️ 仍有未命中仓库评审清单的 migration，已按 ALLOW_REVIEWED_MIGRATION=YES 手工放行"
    else
      printf '%s' "$UNREVIEWED_MIGRATIONS"
      fail "存在未评审或内容哈希已变化的 Flyway migration；请先评审并登记精确 Git blob SHA"
    fi
  else
    log "没有新增 migration（与线上运行版本一致）"
  fi
fi

log "3/8 生产前置检查"
sudo systemctl is-active --quiet xinghuo-v2.service || fail "xinghuo-v2.service 不是 active"
sudo systemctl is-active --quiet nginx || fail "nginx 不是 active"
sudo nginx -t >/dev/null
PREDEPLOY_HOME_MS=$(probe_http_millis "http://127.0.0.1:8080/") || fail "部署前 HOME 健康检查失败"
PREDEPLOY_SERVICES_MS=$(probe_http_millis "http://127.0.0.1:8080/services") || fail "部署前 SERVICES 健康检查失败"
log "部署前 8080 健康基线: HOME=${PREDEPLOY_HOME_MS}ms SERVICES=${PREDEPLOY_SERVICES_MS}ms"
if sudo ss -lntp | grep -q ':8081\b'; then fail "8081 已被占用"; fi
prepare_smoke_port || fail "$SMOKE_PORT 仍被未知或非本次可安全清理的进程占用"
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
prepare_smoke_port || fail "$SMOKE_PORT 仍被未知或非本次可安全清理的进程占用"
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

PREVIOUS_JAR=$(readlink -f "$APP_DIR/current.jar")
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
POSTDEPLOY_HOME_MS=$(probe_http_millis "http://127.0.0.1:8080/") || fail "发布后 HOME 健康检查失败"
POSTDEPLOY_SERVICES_MS=$(probe_http_millis "http://127.0.0.1:8080/services") || fail "发布后 SERVICES 健康检查失败"
log "发布后 8080 健康探针: HOME=${POSTDEPLOY_HOME_MS}ms SERVICES=${POSTDEPLOY_SERVICES_MS}ms"
sudo grep -q '127\.0\.0\.1:8080;' /etc/nginx/conf.d/xinghuo-upstream.conf \
  || fail "发布后 nginx 未切回 8080"
if sudo ss -lntp | grep -q ':8081\b'; then fail "发布后 8081 仍在监听"; fi
if sudo ss -lntp | grep -q ':18080\b'; then fail "发布后 18080 仍在监听"; fi

CURRENT_JAR=$(readlink -f "$APP_DIR/current.jar")
CURRENT_SHA=$(sudo sha256sum "$CURRENT_JAR" | awk '{print $1}')
[ "$CURRENT_SHA" = "$CANDIDATE_SHA" ] || fail "发布后的 current.jar SHA256 与候选包不一致"

# 只有 18080、8081、8080、Nginx 回切、端口清理和 current.jar 校验全部通过后，
# 才原子写入主机侧发布凭证。应用只能读取该文件，不能自行生成成功结论。
ATTESTATION_TMP=$(mktemp)
cat > "$ATTESTATION_TMP" <<EOF
{
  "schemaVersion": 2,
  "source": "production-host-gateway",
  "publishedAt": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')",
  "gitSha": "$HEAD_SHA",
  "jarSha256": "$CANDIDATE_SHA",
  "currentRelease": "$(basename "$CURRENT_JAR")",
  "previousRelease": "$(basename "$PREVIOUS_JAR")",
  "mainPort": 8080,
  "canaryPort": 8081,
  "nginxUpstreamPort": 8080,
  "canaryStopped": true,
  "checks": {
    "predeploy18080": true,
    "canary8081": true,
    "production8080": true
  },
  "healthComparison": {
    "preDeploy8080": {
      "passed": true,
      "homeMillis": $PREDEPLOY_HOME_MS,
      "servicesMillis": $PREDEPLOY_SERVICES_MS
    },
    "postDeploy8080": {
      "passed": true,
      "homeMillis": $POSTDEPLOY_HOME_MS,
      "servicesMillis": $POSTDEPLOY_SERVICES_MS
    }
  }
}
EOF

# 历史发布凭证是主机网关的附加只读审计材料。归档失败不反向伪造生产故障，
# 但会在发布日志中明确告警，后台历史基线也会显示为缺失/构建中。
if sudo install -d -o root -g xinghuo -m 0750 "$RELEASE_ATTESTATION_HISTORY_DIR"; then
  PREVIOUS_HISTORY_NAME="$(basename "$PREVIOUS_JAR" .jar).json"
  if sudo test -r "$RELEASE_ATTESTATION_FILE"; then
    if sudo install -o root -g xinghuo -m 0640 \
      "$RELEASE_ATTESTATION_FILE" "$RELEASE_ATTESTATION_HISTORY_DIR/$PREVIOUS_HISTORY_NAME"; then
      log "✅ 已归档上一版主机发布凭证: $PREVIOUS_HISTORY_NAME"
    else
      log "⚠️ 上一版主机发布凭证归档失败；不影响已完成的生产切流"
    fi
  fi
else
  log "⚠️ 无法准备主机发布历史目录；历史稳定性基线本次不会增长"
fi

sudo install -o root -g xinghuo -m 0640 "$ATTESTATION_TMP" "$RELEASE_ATTESTATION_FILE.next"
sudo mv -Tf "$RELEASE_ATTESTATION_FILE.next" "$RELEASE_ATTESTATION_FILE"
log "✅ 主机侧发布凭证已更新（含发布前后 8080 健康对比）"

CURRENT_HISTORY_NAME="$(basename "$CURRENT_JAR" .jar).json"
if sudo test -d "$RELEASE_ATTESTATION_HISTORY_DIR"; then
  if sudo install -o root -g xinghuo -m 0640 \
    "$ATTESTATION_TMP" "$RELEASE_ATTESTATION_HISTORY_DIR/$CURRENT_HISTORY_NAME.next" \
    && sudo mv -Tf \
      "$RELEASE_ATTESTATION_HISTORY_DIR/$CURRENT_HISTORY_NAME.next" \
      "$RELEASE_ATTESTATION_HISTORY_DIR/$CURRENT_HISTORY_NAME"; then
    log "✅ 当前主机发布凭证已加入历史基线: $CURRENT_HISTORY_NAME"
  else
    sudo rm -f "$RELEASE_ATTESTATION_HISTORY_DIR/$CURRENT_HISTORY_NAME.next" 2>/dev/null || true
    log "⚠️ 当前主机发布凭证归档失败；最新发布凭证本身仍然有效"
  fi
fi
rm -f "$ATTESTATION_TMP"

log "✅ GitHub 一键生产发布完成"
log "   Git SHA: $HEAD_SHA"
log "   JAR SHA256: $CANDIDATE_SHA"
log "   Current: $CURRENT_JAR"
log "   Nginx: 8080"
