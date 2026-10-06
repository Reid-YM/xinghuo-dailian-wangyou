#!/usr/bin/env bash
set -euo pipefail
PORT=${1:-18080}
BASE="http://127.0.0.1:$PORT"
fail=0
check_code() {
  local path=$1 expected=$2
  local code
  code=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE$path" || true)
  if [ "$code" != "$expected" ]; then
    echo "FAIL $path expected=$expected actual=$code"
    fail=1
  else
    echo "OK   $path $code"
  fi
}
check_code / 200
check_code /services 200
check_code /guides 200
check_code /mastery 200
check_code /api/guides 200
check_code /api/tool-station 200
# 用户中心与设置页必须要求登录，未登录时页面跳转到登录页。
check_code /account 302
check_code /account/login 200
check_code /account/register 200
check_code /rules 200
check_code /privacy 200
check_code /contact 200
check_code /records 200
check_code /trust 200
check_code /order 200
check_code /api/account/me 401
check_code /api/account/security/events 401
check_removed() {
  local path=$1
  local code
  code=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE$path" || true)
  case "$code" in
    301|302|307|308|404) echo "OK   $path $code (removed or redirected)" ;;
    *) echo "FAIL $path expected removed/redirect actual=$code"; fail=1 ;;
  esac
}
check_removed /loadouts
check_code /api/loadouts 404
check_code /assets/loadouts.js 404
# 后台旧状态接口仍存在但必须受管理员认证保护，未登录访问应为 401。
check_code /api/admin/ai-chat/status 401
exit "$fail"
