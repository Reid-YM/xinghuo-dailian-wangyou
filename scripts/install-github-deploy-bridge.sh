#!/usr/bin/env bash
# 一次性安装 GitHub Actions -> 生产服务器的受限 SSH 发布桥。
# 用法：
#   sudo bash scripts/install-github-deploy-bridge.sh 'ssh-ed25519 AAAA... comment'
#
# 该脚本不会生成或上传私钥；私钥必须由项目负责人单独保存到 GitHub Actions Secret。
set -Eeuo pipefail

PUBKEY=${1:-}
DEPLOY_USER=${DEPLOY_USER:-ubuntu}
HOME_DIR=${HOME_DIR:-/home/$DEPLOY_USER}
AUTHORIZED_KEYS=${AUTHORIZED_KEYS:-$HOME_DIR/.ssh/authorized_keys}
GATEWAY_SOURCE=${GATEWAY_SOURCE:-/srv/xinghuo-dev/work/xinghuo-v2/scripts/github-deploy-gateway.sh}
GATEWAY_TARGET=${GATEWAY_TARGET:-/usr/local/sbin/xinghuo-github-deploy-gateway}

fail() { echo "❌ $*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || fail "请使用 sudo 运行"
[ -n "$PUBKEY" ] || fail "缺少 SSH 公钥参数"
[[ "$PUBKEY" != *$'\n'* ]] || fail "公钥参数不能包含换行"
[[ "$PUBKEY" =~ ^ssh-(ed25519|rsa)[[:space:]]+ ]] || fail "仅接受标准 ssh-ed25519 或 ssh-rsa 公钥"
id "$DEPLOY_USER" >/dev/null 2>&1 || fail "用户不存在: $DEPLOY_USER"
[ -f "$GATEWAY_SOURCE" ] || fail "找不到 gateway 源文件: $GATEWAY_SOURCE"

install -o root -g root -m 0755 "$GATEWAY_SOURCE" "$GATEWAY_TARGET"

install -d -o "$DEPLOY_USER" -g "$DEPLOY_USER" -m 0700 "$HOME_DIR/.ssh"
touch "$AUTHORIZED_KEYS"
chown "$DEPLOY_USER:$DEPLOY_USER" "$AUTHORIZED_KEYS"
chmod 0600 "$AUTHORIZED_KEYS"

BACKUP="$AUTHORIZED_KEYS.before-xinghuo-github-deploy-$(date +%Y%m%d%H%M%S)"
cp -a "$AUTHORIZED_KEYS" "$BACKUP"

KEY_BODY=$(printf '%s\n' "$PUBKEY" | awk '{print $1" "$2}')
ENTRY="command=\"$GATEWAY_TARGET\",no-agent-forwarding,no-port-forwarding,no-X11-forwarding,no-pty $PUBKEY"

if grep -Fq "$KEY_BODY" "$AUTHORIZED_KEYS"; then
  echo "⚠️  该公钥已存在于 authorized_keys，未重复添加。"
else
  printf '%s\n' "$ENTRY" >> "$AUTHORIZED_KEYS"
  chown "$DEPLOY_USER:$DEPLOY_USER" "$AUTHORIZED_KEYS"
  chmod 0600 "$AUTHORIZED_KEYS"
fi

echo "✅ GitHub 生产部署桥已安装"
echo "用户: $DEPLOY_USER"
echo "Gateway: $GATEWAY_TARGET"
echo "authorized_keys 备份: $BACKUP"
echo "该部署密钥只能执行: deploy <40位commit-sha>"
