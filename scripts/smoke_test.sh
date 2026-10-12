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
check_code / 200
check_code /services 200
check_code /guides 200
check_code /mastery 200
check_code /api/guides 200
check_code /api/tool-station 200
# 匿名可索引页面不能再因浏览器语言或爬虫来源变化而改变 canonical URL 下的语言。
check_header /services Content-Language zh-CN -H 'Accept-Language: en-US,en;q=0.9'
# 用户主动选择英文后仍应通过 Cookie 保留英文体验。
check_header /services Content-Language en -H 'Cookie: xh_lang=en'
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
