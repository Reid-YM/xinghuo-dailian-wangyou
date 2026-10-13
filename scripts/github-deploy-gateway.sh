#!/usr/bin/env bash
# 生产 SSH 部署密钥的 forced-command 网关。
# 建议由 install-github-deploy-bridge.sh 安装到 /usr/local/sbin/xinghuo-github-deploy-gateway。
# 该网关只接受：deploy <40位commit-sha>
set -Eeuo pipefail

WORK=${WORK:-/srv/xinghuo-dev/work/xinghuo-v2}
ORIGINAL=${SSH_ORIGINAL_COMMAND:-}

fail() {
  echo "DEPLOY_GATEWAY_REJECTED: $*" >&2
  exit 1
}

[[ "$ORIGINAL" =~ ^deploy[[:space:]]+([0-9a-f]{40})$ ]] \
  || fail "only 'deploy <40-char-sha>' is allowed"

TARGET_SHA=${BASH_REMATCH[1]}

[ -d "$WORK/.git" ] || fail "production workspace is not a Git repository"
[ "$(git -C "$WORK" branch --show-current)" = "main" ] \
  || fail "production workspace is not on main"
[ -z "$(git -C "$WORK" status --porcelain)" ] \
  || fail "production workspace is dirty"

git -C "$WORK" fetch origin main
REMOTE_SHA=$(git -C "$WORK" rev-parse origin/main)
[ "$REMOTE_SHA" = "$TARGET_SHA" ] \
  || fail "origin/main does not match requested SHA"

git -C "$WORK" pull --ff-only origin main
HEAD_SHA=$(git -C "$WORK" rev-parse HEAD)
[ "$HEAD_SHA" = "$TARGET_SHA" ] \
  || fail "local main does not match requested SHA after pull"
[ -z "$(git -C "$WORK" status --porcelain)" ] \
  || fail "production workspace became dirty after pull"

CONFIRM_GITHUB_DEPLOY=YES \
  bash "$WORK/scripts/deploy-from-github.sh" "$TARGET_SHA"
