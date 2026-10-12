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
check_header() {
  local path=$1 header=$2 expected=$3
  shift 3
  local value
  value=$(curl -sS -D - -o /dev/null "$@" "$BASE$path" \
    | tr -d '\r' \
    | awk -v h="$header" 'BEGIN{IGNORECASE=1} $0 ~ "^" h ":" {sub(/^[^:]+:[[:space:]]*/, ""); print; exit}')
  if [ "$value" != "$expected" ]; then
    echo "FAIL $path header=$header expected=$expected actual=${value:-<missing>}"
    fail=1
  else
    echo "OK   $path $header=$value"
  fi
}
check_body_contains() {
  local path=$1 expected=$2
  shift 2
  local body
  body=$(curl -sS "$@" "$BASE$path" || true)
  if ! printf '%s' "$body" | grep -Fq "$expected"; then
    echo "FAIL $path body missing: $expected"
    fail=1
  else
    echo "OK   $path body contains: $expected"
  fi
}
check_code / 200
check_code /services 200
check_code /guides 200
check_code /mastery 200
check_code /api/guides 200
check_code /api/tool-station 200
# 所有可索引 canonical 页面必须稳定输出中文正文，而不是只把响应头写成 zh-CN。
# 即使请求来自英语浏览器，或携带历史 xh_lang=en Cookie，也不能把同一 URL 翻成英文。
check_header /services Content-Language zh-CN -H 'Accept-Language: en-US,en;q=0.9'
check_body_contains /services '<html lang="zh-CN">' -H 'Accept-Language: en-US,en;q=0.9'
check_body_contains /services '服务大厅' -H 'Accept-Language: en-US,en;q=0.9'
check_header /services Content-Language zh-CN -H 'Cookie: xh_lang=en'
check_body_contains /services '<html lang="zh-CN">' -H 'Cookie: xh_lang=en'
check_body_contains /services '服务大厅' -H 'Cookie: xh_lang=en'
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
check_code /rules 200
check_code /rules-en 200
check_code /privacy 200
check_code /privacy-en 200
check_code '/language?lang=en&returnTo=/rules' 302
check_header '/language?lang=en&returnTo=/rules' X-Robots-Tag 'noindex, nofollow'
check_code /contact 200
check_code /records 200
check_header /records X-Robots-Tag 'noindex, follow'
check_code /trust 200
# 旧业务查询 URL 必须永久归并到当前 canonical 工具地址，不能继续返回第二套 200 页面。
check_code /order 301
check_header /order Location '/records'
check_header /order X-Robots-Tag 'noindex, follow'
check_code /order/XH7KM4Q2AZ8P 301
check_header /order/XH7KM4Q2AZ8P Location '/record/XH7KM4Q2AZ8P'
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
