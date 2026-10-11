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
check_redirect() {
  local path=$1 expected_location=$2
  local headers code location
  headers=$(curl -sS -D - -o /dev/null "$BASE$path" || true)
  code=$(printf '%s\n' "$headers" | awk 'NR==1 {print $2}')
  location=$(printf '%s\n' "$headers" | awk 'BEGIN{IGNORECASE=1} /^Location:/ {sub(/\r$/, ""); sub(/^[^:]*:[[:space:]]*/, ""); print; exit}')
  if [ "$code" != "302" ] || [[ "$location" != *"$expected_location" ]]; then
    echo "FAIL $path expected=302->$expected_location actual=${code:-none}->${location:-none}"
    fail=1
  else
    echo "OK   $path 302 -> $location"
  fi
}
check_code / 200
check_code /services 200
check_code /guides 200
check_code /mastery 200
check_code /api/guides 200
check_code /api/tool-station 200
# 用户中心所有真实页面都必须存在并要求登录。未登录时应跳转登录页，绝不能 404。
check_code /account 302
check_code /account/requests 302
check_code /account/orders 302
check_code /account/progress 302
check_code /account/settings 302
check_code /account/submit 302
# 常见旧用户入口也必须被账号拦截器接住，登录后再重定向到 canonical /account/**。
check_code /account/quotes 302
check_code /account/profile 302
check_code /account/security 302
check_code /console 302
check_code /console/overview 302
check_code /console/profile 302
check_code /console/quotes 302
check_code /console/orders 302
check_code /console/security 302
check_code /account/login 200
check_code /account/register 200
# 后台入口是生产关键路径：登录页必须可访问，未登录访问 /admin 必须明确回登录页。
check_code /admin/login 200
check_redirect /admin /admin/login
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