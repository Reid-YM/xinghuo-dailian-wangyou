#!/usr/bin/env bash
# =============================================================================
# 星火 V2 零丢包（蓝绿）发布
#
# 而是先在 8081 起一个金丝雀实例，健康后再由 nginx 热切换流量，
# 全程对外零中断（仅有两次 nginx 优雅 reload，在途请求会正常完成）。
#
# 流程：
#   1. 构建新 jar（基线 JAR + 覆盖源码）
#   2. 安装到 releases/
#   3. 在 CANARY_PORT 启动金丝雀实例并健康检查
#   4. nginx upstream 切到金丝雀（reload）
#   5. 停旧服务 -> 切换 current.jar 软链 -> 起正式服务（新版本，MAIN_PORT）
#   6. 健康检查通过后 nginx upstream 切回 MAIN_PORT（reload）
#   7. 停金丝雀、清理旧 release
#
# 任一环节失败都会保持/恢复原有可用状态，不会让站点不可访问。
#
# 用法：sudo bash deploy-zero-downtime.sh
# =============================================================================
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
APP_DIR=${APP_DIR:-/opt/xinghuo-v2}
RELEASES_DIR=${RELEASES_DIR:-$APP_DIR/releases}
BASE_JAR=${BASE_JAR:-/srv/xinghuo-dev/base/xinghuo-v2-base.jar}
SERVICE=${SERVICE:-xinghuo-v2.service}
APP_LINK=${APP_LINK:-$APP_DIR/current.jar}
UPSTREAM_CONF=${UPSTREAM_CONF:-/etc/nginx/conf.d/xinghuo-upstream.conf}
CANARY_UNIT=${CANARY_UNIT:-xinghuo-canary}
CANARY_PORT=${CANARY_PORT:-8081}
MAIN_PORT=${MAIN_PORT:-8080}
KEEP_RELEASES=${KEEP_RELEASES:-5}

TS=$(date +%Y%m%d%H%M%S)
BUILD_JAR="$ROOT/build/xinghuo-v2-$TS.jar"
RELEASE_JAR="$RELEASES_DIR/xinghuo-v2-$TS.jar"
PREVIOUS_JAR=""
CANARY_STARTED=0
UPSTREAM_PORT="$MAIN_PORT"

log()  { echo "[$(date '+%F %T')] $*"; }
fail() { log "❌ $*"; exit 1; }

cleanup_canary() {
  if [ "$CANARY_STARTED" = "1" ]; then
    # 安全阀：如果 nginx 仍指向金丝雀端口，绝不能停它，否则站点会不可访问
    if grep -q "127\.0\.0\.1:${CANARY_PORT};" "$UPSTREAM_CONF" 2>/dev/null; then
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
  curl -fsS --max-time 4 "http://127.0.0.1:$port/"      >/dev/null 2>&1 || return 1
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

switch_upstream() {
  local port=$1
  sudo sed -i -E "s|(server 127\.0\.0\.1:)[0-9]+;|\1${port};|" "$UPSTREAM_CONF"
  sudo nginx -t >/dev/null 2>&1 || { log "❌ nginx 语法检查失败，保持原状"; return 1; }
  sudo systemctl reload nginx
  UPSTREAM_PORT="$port"
  log "nginx 流量已切到 127.0.0.1:$port"
}

# ---------------------------------------------------------------- 0. 前置检查
[ -f "$BASE_JAR" ] || fail "找不到基线 JAR: $BASE_JAR"
[ -f "$UPSTREAM_CONF" ] || fail "找不到 upstream 配置: $UPSTREAM_CONF（请先完成 nginx upstream 改造）"
sudo systemctl is-active --quiet nginx || fail "nginx 未运行"

PREVIOUS_JAR=$(readlink -f "$APP_LINK" 2>/dev/null || true)
log "当前线上版本: ${PREVIOUS_JAR:-未知}"
healthy "$MAIN_PORT" || fail "发布前线上服务健康检查未通过，先排查再发布"

# 防呆：确认 nginx 实际加载的配置确实走了 upstream。
# 踩过的坑：/etc/nginx/sites-enabled/ 下可能是普通文件副本而不是指向 sites-available 的软链，
# 只改 sites-available 不会生效，切流会静默失败并造成 502。
# 注意：这里不能用 `nginx -T | grep -q`——在本脚本的 set -o pipefail 下，
# grep -q 提前退出会让 nginx 收到 SIGPIPE（141）从而被误判为失败。先取全量输出再匹配。
NGINX_DUMP=$(sudo nginx -T 2>/dev/null || true)
case "$NGINX_DUMP" in
  *"proxy_pass http://xinghuo_backend;"*)
    log "nginx upstream 校验通过" ;;
  *)
    fail "nginx 生效配置未使用 xinghuo_backend upstream。
       请检查 /etc/nginx/sites-enabled/ 下的实际文件（可能是副本而非软链）并把 proxy_pass 指向 upstream。" ;;
esac

# ---------------------------------------------------------------- 1. 构建
log "构建新版本（基线 JAR + 覆盖源码）"
BASE_JAR="$BASE_JAR" bash "$ROOT/scripts/dev_build.sh" "$BUILD_JAR" >/dev/null
[ -f "$BUILD_JAR" ] || fail "构建未产出 jar"
log "构建完成: $(du -h "$BUILD_JAR" | cut -f1)"

# ---------------------------------------------------------------- 2. 安装
sudo install -d -o root -g root -m 0755 "$RELEASES_DIR"
sudo install -o xinghuo -g xinghuo -m 0640 "$BUILD_JAR" "$RELEASE_JAR"
log "已安装: $RELEASE_JAR"

# ---------------------------------------------------------------- 3. 金丝雀
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
  log "金丝雀健康检查失败，线上未受任何影响"
  sudo journalctl -u "$CANARY_UNIT" --no-pager -n 25 || true
  fail "新版本未通过健康检查"
fi
log "✅ 金丝雀就绪"

# ---------------------------------------------------------------- 4. 切流
switch_upstream "$CANARY_PORT" || fail "切换 upstream 失败"
sleep 3   # 等在途请求处理完

# ---------------------------------------------------------------- 5. 正式服务
log "停旧服务 -> 切换 current.jar -> 启动正式服务"
sudo systemctl stop "$SERVICE"
sudo ln -sfn "$RELEASE_JAR" "$APP_LINK.next"
sudo mv -Tf "$APP_LINK.next" "$APP_LINK"
sudo systemctl start "$SERVICE"

if ! wait_healthy "$MAIN_PORT" 60; then
  log "正式服务启动失败，回滚到 $PREVIOUS_JAR"
  sudo journalctl -u "$SERVICE" --no-pager -n 30 || true
  sudo systemctl stop "$SERVICE" || true
  if [ -n "$PREVIOUS_JAR" ]; then
    sudo ln -sfn "$PREVIOUS_JAR" "$APP_LINK.next"
    sudo mv -Tf "$APP_LINK.next" "$APP_LINK"
    sudo systemctl start "$SERVICE"
    if wait_healthy "$MAIN_PORT" 45; then
      switch_upstream "$MAIN_PORT" || true
      fail "已回滚到部署前版本，站点正常"
    fi
  fi
  fail "回滚后健康检查仍未通过，请人工介入（nginx 仍指向金丝雀 :$CANARY_PORT）"
fi
log "✅ 正式服务已运行新版本"

# ---------------------------------------------------------------- 6. 切回
switch_upstream "$MAIN_PORT" || fail "切回主端口失败"
sleep 2

# ---------------------------------------------------------------- 7. 清理
cleanup_canary
log "清理过期 release（保留 $KEEP_RELEASES 份，且不删当前与上一版）"
find "$RELEASES_DIR" -maxdepth 1 -type f -name 'xinghuo-v2-*.jar' -printf '%T@ %p\n' 2>/dev/null \
  | sort -nr \
  | awk -v keep="$KEEP_RELEASES" -v cur="$(readlink -f "$APP_LINK")" -v prev="$PREVIOUS_JAR" \
      'NR>keep && $2!=cur && $2!=prev {print $2}' \
  | xargs -r sudo rm -f

log "✅ 发布完成（全程零中断）"
log "   当前版本: $(readlink -f "$APP_LINK")"
log "   上一版本: ${PREVIOUS_JAR:-无}"
log "   回滚命令: sudo ln -sfn $PREVIOUS_JAR $APP_LINK.next && sudo mv -Tf $APP_LINK.next $APP_LINK && sudo systemctl restart $SERVICE"
