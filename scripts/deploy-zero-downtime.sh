#!/usr/bin/env bash
# =============================================================================
# 星火 V2 零停机（蓝绿）发布
#
# 本脚本只负责发布“已经完成 Maven 构建并通过 18080 预发布冒烟”的 JAR。
# 它不再使用 /srv/xinghuo-dev/base/xinghuo-v2-base.jar，也不在发布阶段重新构建源码。
#
# 正式流程：
#   1. GitHub main -> /opt/xinghuo-v2/source-current
#   2. mvn clean package
#   3. 候选 JAR 在 18080 启动并执行 scripts/smoke_test.sh 18080
#   4. 将该 JAR 的 SHA256 通过 PREDEPLOY_SMOKE_SHA256 传入本脚本
#   5. 安装为 releases/xinghuo-v2-standard-<timestamp>.jar
#   6. 在 8081 启动金丝雀并执行完整冒烟
#   7. nginx 热切流到 8081
#   8. 原子切换 current.jar，启动正式 8080 并再次执行完整冒烟
#   9. nginx 切回 8080，停止金丝雀
#
# 用法示例：
#   JAR=target/xinghuo-v2-1.0.0.jar
#   SHA=$(sha256sum "$JAR" | awk '{print $1}')
#   sudo env \
#     CONFIRM_PROD_DEPLOY=YES \
#     PREDEPLOY_SMOKE_SHA256="$SHA" \
#     DEPLOY_GIT_SHA="$(git rev-parse HEAD)" \
#     bash scripts/deploy-zero-downtime.sh "$JAR"
#
# 注意：包含 Flyway 迁移的版本必须先确认迁移可与旧版并行兼容。
# 对破坏性或不可逆迁移，不得直接使用本零停机流程。
# =============================================================================
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
CANDIDATE_INPUT=${1:-}
APP_DIR=${APP_DIR:-/opt/xinghuo-v2}
RELEASES_DIR=${RELEASES_DIR:-$APP_DIR/releases}
SERVICE=${SERVICE:-xinghuo-v2.service}
APP_LINK=${APP_LINK:-$APP_DIR/current.jar}
UPSTREAM_CONF=${UPSTREAM_CONF:-/etc/nginx/conf.d/xinghuo-upstream.conf}
CANARY_UNIT=${CANARY_UNIT:-xinghuo-canary}
CANARY_PORT=${CANARY_PORT:-8081}
MAIN_PORT=${MAIN_PORT:-8080}
KEEP_RELEASES=${KEEP_RELEASES:-5}
SMOKE_TEST=${SMOKE_TEST:-$ROOT/scripts/smoke_test.sh}

TS=$(date +%Y%m%d%H%M%S)
RELEASE_JAR="$RELEASES_DIR/xinghuo-v2-standard-$TS.jar"
PREVIOUS_JAR=""
PREVIOUS_FRONTEND_MODE=""
CANARY_STARTED=0
UPSTREAM_PORT="$MAIN_PORT"
DEPLOY_GIT_SHA=${DEPLOY_GIT_SHA:-}

log()  { echo "[$(date '+%F %T')] $*"; }
fail() { log "❌ $*"; exit 1; }

cleanup_canary() {
  if [ "$CANARY_STARTED" = "1" ]; then
    # 安全阀：如果 nginx 仍指向金丝雀端口，绝不能停它，否则会造成站点不可访问。
    if sudo grep -q "127\.0\.0\.1:${CANARY_PORT};" "$UPSTREAM_CONF" 2>/dev/null; then
      log "⚠️  nginx 仍指向金丝雀 :$CANARY_PORT，为避免中断，保留该实例运行"
      return 0
    fi
    log "停止金丝雀实例"
    sudo systemctl stop "$CANARY_UNIT" 2>/dev/null || true
    sudo systemctl reset-failed "$CANARY_UNIT" 2>/dev/null || true
    CANARY_STARTED=0
  fi
}
trap cleanup_canary EXIT

healthy() {
  local port=$1
  curl -fsS --max-time 4 "http://127.0.0.1:$port/" >/dev/null 2>&1 || return 1
  curl -fsS --max-time 4 "http://127.0.0.1:$port/services" >/dev/null 2>&1 || return 1
  return 0
}

wait_healthy() {
  local port=$1 tries=${2:-45} i
  for ((i = 1; i <= tries; i++)); do
    healthy "$port" && return 0
    sleep 2
  done
  return 1
}

run_smoke() {
  local port=$1 expected_mode=${2:-${FRONTEND_EXPECTED_MODE:-next}}
  [ -f "$SMOKE_TEST" ] || fail "找不到冒烟脚本: $SMOKE_TEST"
  FRONTEND_EXPECTED_MODE="$expected_mode" bash "$SMOKE_TEST" "$port"
}

# Record the serving version before any cutover; rollback must verify that version's view.
frontend_mode() {
  local header
  header=$(curl -fsS --max-time 8 -D - -o /dev/null "http://127.0.0.1:$1/" \
    | tr -d '\r' \
    | awk 'tolower($0) ~ /^x-xinghuo-frontend:/ {sub(/^[^:]+:[[:space:]]*/, ""); print; exit}') || return 1
  case "$header" in
    next) echo next ;;
    '') echo legacy ;;
    *) return 1 ;;
  esac
}

switch_upstream() {
  local port=$1
  local backup="$UPSTREAM_CONF.xinghuo-deploy-$TS.bak"

  sudo cp -a "$UPSTREAM_CONF" "$backup"

  if ! sudo sed -i -E "s|(server 127\.0\.0\.1:)[0-9]+;|\1${port};|" "$UPSTREAM_CONF"; then
    sudo cp -a "$backup" "$UPSTREAM_CONF" || true
    sudo rm -f "$backup" || true
    return 1
  fi

  if ! sudo grep -q "127\.0\.0\.1:${port};" "$UPSTREAM_CONF"; then
    sudo cp -a "$backup" "$UPSTREAM_CONF" || true
    sudo rm -f "$backup" || true
    log "❌ upstream 配置中未找到目标端口 :$port，已恢复配置文件"
    return 1
  fi

  if ! sudo nginx -t >/dev/null 2>&1; then
    sudo cp -a "$backup" "$UPSTREAM_CONF" || true
    sudo nginx -t >/dev/null 2>&1 || true
    sudo rm -f "$backup" || true
    log "❌ nginx 语法检查失败，已恢复 upstream 配置文件"
    return 1
  fi

  if ! sudo systemctl reload nginx; then
    sudo cp -a "$backup" "$UPSTREAM_CONF" || true
    sudo nginx -t >/dev/null 2>&1 || true
    sudo systemctl reload nginx >/dev/null 2>&1 || true
    sudo rm -f "$backup" || true
    log "❌ nginx reload 失败，已尝试恢复 upstream 配置"
    return 1
  fi

  sudo rm -f "$backup"
  UPSTREAM_PORT="$port"
  log "nginx 流量已切到 127.0.0.1:$port"
}

rollback_main() {
  log "回滚正式 8080 到部署前版本: $PREVIOUS_JAR"
  sudo systemctl stop "$SERVICE" || true
  sudo ln -sfn "$PREVIOUS_JAR" "$APP_LINK.next"
  sudo mv -Tf "$APP_LINK.next" "$APP_LINK"
  sudo systemctl start "$SERVICE"

  if wait_healthy "$MAIN_PORT" 45 && run_smoke "$MAIN_PORT" "$PREVIOUS_FRONTEND_MODE"; then
    if switch_upstream "$MAIN_PORT"; then
      log "↩️  已回滚到部署前版本，8080 与 nginx 均正常"
      return 0
    fi
  fi

  log "⚠️  回滚后正式 8080 未完全恢复；若 nginx 仍指向金丝雀，将保留金丝雀继续承载流量"
  return 1
}

# ---------------------------------------------------------------- 0. 发布权限与输入检查
[ "${CONFIRM_PROD_DEPLOY:-}" = "YES" ] || fail "未获得生产发布确认。请在明确确认部署后设置 CONFIRM_PROD_DEPLOY=YES"
[ -n "$CANDIDATE_INPUT" ] || fail "用法: sudo env CONFIRM_PROD_DEPLOY=YES PREDEPLOY_SMOKE_SHA256=<sha256> DEPLOY_GIT_SHA=<git-sha> bash $0 <candidate.jar>"
[ -f "$CANDIDATE_INPUT" ] || fail "找不到候选 JAR: $CANDIDATE_INPUT"
[ -r "$CANDIDATE_INPUT" ] || fail "候选 JAR 不可读: $CANDIDATE_INPUT"
[ -f "$SMOKE_TEST" ] || fail "找不到冒烟脚本: $SMOKE_TEST"
[ -f "$UPSTREAM_CONF" ] || fail "找不到 upstream 配置: $UPSTREAM_CONF"

CANDIDATE_JAR=$(readlink -f "$CANDIDATE_INPUT")
CANDIDATE_SHA256=$(sha256sum "$CANDIDATE_JAR" | awk '{print $1}')
[ -n "${PREDEPLOY_SMOKE_SHA256:-}" ] || fail "缺少 PREDEPLOY_SMOKE_SHA256；必须先完成 18080 冒烟"
[ "$PREDEPLOY_SMOKE_SHA256" = "$CANDIDATE_SHA256" ] || fail "18080 冒烟记录与当前候选 JAR SHA256 不一致"

# ---------------------------------------------------------------- 1. Git / 线上前置检查
if ! git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  fail "部署脚本必须从正式 Git 工作区执行: $ROOT"
fi

CURRENT_BRANCH=$(git -C "$ROOT" branch --show-current)
CURRENT_GIT_SHA=$(git -C "$ROOT" rev-parse HEAD)
[ "$CURRENT_BRANCH" = "main" ] || fail "生产发布只允许来自 main；当前分支: $CURRENT_BRANCH"
[ -z "$(git -C "$ROOT" status --porcelain)" ] || fail "生产源码工作区不干净，停止发布"

if [ -n "$DEPLOY_GIT_SHA" ] && [ "$DEPLOY_GIT_SHA" != "$CURRENT_GIT_SHA" ]; then
  fail "DEPLOY_GIT_SHA 与当前源码 HEAD 不一致: expected=$DEPLOY_GIT_SHA actual=$CURRENT_GIT_SHA"
fi
DEPLOY_GIT_SHA="$CURRENT_GIT_SHA"

sudo systemctl is-active --quiet nginx || fail "nginx 未运行"
sudo systemctl is-active --quiet "$SERVICE" || fail "$SERVICE 未运行"
if sudo systemctl is-active --quiet "$CANARY_UNIT"; then
  fail "检测到已有金丝雀单元 $CANARY_UNIT 正在运行，请先人工确认现场"
fi

PREVIOUS_JAR=$(readlink -f "$APP_LINK" 2>/dev/null || true)
[ -n "$PREVIOUS_JAR" ] || fail "无法解析当前线上 JAR: $APP_LINK"
[ -f "$PREVIOUS_JAR" ] || fail "当前线上 JAR 不存在: $PREVIOUS_JAR"
healthy "$MAIN_PORT" || fail "发布前正式 :$MAIN_PORT 健康检查未通过"
PREVIOUS_FRONTEND_MODE=$(frontend_mode "$MAIN_PORT") || fail "无法确认部署前前端模式"
log "部署前前端模式: $PREVIOUS_FRONTEND_MODE（仅供回滚验收）"

# 发布开始前必须确保 nginx 正在指向正式端口，而不是遗留金丝雀端口。
sudo grep -q "127\.0\.0\.1:${MAIN_PORT};" "$UPSTREAM_CONF" || fail "nginx upstream 当前未指向正式 :$MAIN_PORT，请先人工确认现场"

NGINX_DUMP=$(sudo nginx -T 2>/dev/null || true)
case "$NGINX_DUMP" in
  *"proxy_pass http://xinghuo_backend;"*)
    log "nginx upstream 校验通过" ;;
  *)
    fail "nginx 生效配置未使用 xinghuo_backend upstream" ;;
esac

log "部署 Git SHA: $DEPLOY_GIT_SHA"
log "候选 JAR: $CANDIDATE_JAR"
log "候选 SHA256: $CANDIDATE_SHA256"
log "当前线上版本: $PREVIOUS_JAR"

# ---------------------------------------------------------------- 2. 安装已验证候选 JAR
sudo install -d -o root -g root -m 0755 "$RELEASES_DIR"
sudo install -o xinghuo -g xinghuo -m 0640 "$CANDIDATE_JAR" "$RELEASE_JAR"
RELEASE_SHA256=$(sudo sha256sum "$RELEASE_JAR" | awk '{print $1}')
[ "$RELEASE_SHA256" = "$CANDIDATE_SHA256" ] || fail "release JAR SHA256 校验失败"
log "已安装 release: $RELEASE_JAR"

# ---------------------------------------------------------------- 3. 8081 金丝雀
log "在 :$CANARY_PORT 启动金丝雀实例（与线上并存）"
sudo systemctl reset-failed "$CANARY_UNIT" 2>/dev/null || true
sudo systemd-run --unit="$CANARY_UNIT" --collect \
  --uid=xinghuo --gid=xinghuo \
  --working-directory="$APP_DIR" \
  --property=EnvironmentFile="$APP_DIR/.env" \
  --property=Restart=no \
  /usr/bin/java -Xms192m -Xmx512m -jar "$RELEASE_JAR" --server.port="$CANARY_PORT" >/dev/null
CANARY_STARTED=1

if ! wait_healthy "$CANARY_PORT" 60; then
  log "金丝雀基础健康检查失败，线上未切流"
  sudo journalctl -u "$CANARY_UNIT" --no-pager -n 40 || true
  fail "新版本未通过 8081 健康检查"
fi

if ! run_smoke "$CANARY_PORT"; then
  log "金丝雀完整冒烟失败，线上未切流"
  sudo journalctl -u "$CANARY_UNIT" --no-pager -n 40 || true
  fail "新版本未通过 8081 完整冒烟"
fi
log "✅ 8081 金丝雀完整冒烟通过"

# ---------------------------------------------------------------- 4. nginx 切流到金丝雀
switch_upstream "$CANARY_PORT" || fail "切换 upstream 到金丝雀失败"
sleep 3

# ---------------------------------------------------------------- 5. 原子切换 current.jar 并启动正式 8080
log "停旧服务 -> 原子切换 current.jar -> 启动正式服务"
sudo systemctl stop "$SERVICE"
sudo ln -sfn "$RELEASE_JAR" "$APP_LINK.next"
sudo mv -Tf "$APP_LINK.next" "$APP_LINK"
sudo systemctl start "$SERVICE"

if ! wait_healthy "$MAIN_PORT" 60; then
  log "正式服务基础健康检查失败"
  sudo journalctl -u "$SERVICE" --no-pager -n 50 || true
  rollback_main || true
  fail "正式 8080 启动失败；已执行回滚流程"
fi

if ! run_smoke "$MAIN_PORT"; then
  log "正式服务完整冒烟失败"
  sudo journalctl -u "$SERVICE" --no-pager -n 50 || true
  rollback_main || true
  fail "正式 8080 冒烟失败；已执行回滚流程"
fi
log "✅ 正式 8080 完整冒烟通过"

# ---------------------------------------------------------------- 6. nginx 切回正式端口
switch_upstream "$MAIN_PORT" || fail "切回正式 :$MAIN_PORT 失败；金丝雀将保留承载流量"
sleep 2

# ---------------------------------------------------------------- 7. 停金丝雀并清理旧 standard release
cleanup_canary
log "清理过期 standard release（保留 $KEEP_RELEASES 份，且不删当前与上一版）"
find "$RELEASES_DIR" -maxdepth 1 -type f -name 'xinghuo-v2-standard-*.jar' -printf '%T@ %p\n' 2>/dev/null \
  | sort -nr \
  | awk -v keep="$KEEP_RELEASES" -v cur="$(readlink -f "$APP_LINK")" -v prev="$PREVIOUS_JAR" \
      'NR>keep && $2!=cur && $2!=prev {print $2}' \
  | xargs -r sudo rm -f

log "✅ 发布完成"
log "   Git SHA: $DEPLOY_GIT_SHA"
log "   SHA256: $CANDIDATE_SHA256"
log "   当前版本: $(readlink -f "$APP_LINK")"
log "   上一版本: $PREVIOUS_JAR"
log "   正式端口: $MAIN_PORT"
log "   金丝雀端口: $CANARY_PORT（已停止）"
log "   回滚命令: sudo ln -sfn $PREVIOUS_JAR $APP_LINK.next && sudo mv -Tf $APP_LINK.next $APP_LINK && sudo systemctl restart $SERVICE"
