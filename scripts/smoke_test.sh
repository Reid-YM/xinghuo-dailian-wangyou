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
check_contains() {
  local path=$1 needle=$2 body
  body=$(curl -fsS "$BASE$path" || true)
  if [[ "$body" != *"$needle"* ]]; then
    echo "FAIL $path missing=$needle"
    fail=1
  else
    echo "OK   $path contains=$needle"
  fi
}
check_request_id() {
  local path=$1 requested=$2 returned
  returned=$(curl -sS -D - -o /dev/null -H "X-Request-Id: $requested" "$BASE$path" \
    | tr -d '\r' \
    | awk 'BEGIN{IGNORECASE=1} /^X-Request-Id:/ {print $2; exit}')
  if [ "$returned" != "$requested" ]; then
    echo "FAIL $path request-id expected=$requested actual=${returned:-missing}"
    fail=1
  else
    echo "OK   $path request-id=$returned"
  fi
}
check_code / 200
check_code /services 200
check_code /quote 200
check_code /records 200
check_code /guides 200
check_code /mastery 200
check_code /api/site-config 200
check_code /api/guides 200
check_code /api/tool-station 200
check_request_id /api/site-config smoke-request-1300

# 已批准的 VNext 正式接管核心页面壳：路由仍由 Spring/Thymeleaf 控制，页面挂载 React。
check_contains / '<div id="root"></div>'
check_contains /services '<div id="root"></div>'
check_contains /quote '<div id="root"></div>'
check_contains /records '<div id="root"></div>'
check_code /vnext/assets/app.js 200
check_code /vnext/assets/app.css 200
check_code /assets/vnext-formal-ui.css 200
check_code /assets/vnext-public-finish-v4.css 200
check_code /assets/vnext-final-qa-fix-v5.css 200
check_code /assets/vnext-mobile-focus-v7.css 200
check_code /assets/vnext-mobile-polish-v8.css 200

# 共享 legacy bridge 仅继续服务记录/后台；服务详情、投稿页和个人中心已迁移为 feature-owned canonical assets。
# 该 smoke 会依次在 18080、8081、8080 执行，因此三道发布门都会验证它们。
check_code /assets/vnext-legacy-bridge-v3.css 200
check_code /assets/service-detail.css 200
check_code /assets/account-submit.css 200
check_code /assets/account-center.css 200
check_code /assets/account-center-refine.css 200

# /vnext/ 继续作为 noindex 的直接预览入口和故障排查入口。
check_code /vnext/ 200
check_contains /vnext/ '<div id="root"></div>'
VNEXT_HTML=$(curl -fsS "$BASE/vnext/" || true)
VNEXT_JS=$(printf '%s' "$VNEXT_HTML" | grep -oE '/vnext/assets/[^" ]+\.js' | head -n 1 || true)
VNEXT_CSS=$(printf '%s' "$VNEXT_HTML" | grep -oE '/vnext/assets/[^" ]+\.css' | head -n 1 || true)
if [ -z "$VNEXT_JS" ]; then
  echo "FAIL /vnext/ missing bundled JavaScript asset"
  fail=1
else
  check_code "$VNEXT_JS" 200
fi
if [ -z "$VNEXT_CSS" ]; then
  echo "FAIL /vnext/ missing bundled CSS asset"
  fail=1
else
  check_code "$VNEXT_CSS" 200
fi

# 用户中心所有真实页面都必须存在并要求登录。未登录时应跳转登录页，绝不能 404。
check_code /account 302
check_code /account/requests 302
check_code /account/orders 302
check_code /account/progress 302
check_code /account/settings 302
check_code /account/submit 302
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
check_code /account/reset 200
check_contains /account/login 'vnext-public-finish-v4.css?v=20261010ui04'
check_contains /account/login 'vnext-final-qa-fix-v5.css?v=20261010ui05'
check_contains /account/login 'vnext-mobile-focus-v7.css?v=20261010ui07'
check_contains /account/login 'vnext-mobile-polish-v8.css?v=20261010ui08'
check_contains /guides 'vnext-public-finish-v4.css?v=20261010ui04'
check_contains /guides 'vnext-final-qa-fix-v5.css?v=20261010ui05'
check_contains /guides 'vnext-mobile-focus-v7.css?v=20261010ui07'
check_contains /guides 'vnext-mobile-polish-v8.css?v=20261010ui08'
check_code /rules 200
check_code /privacy 200
check_code /contact 200
check_code /trust 200
check_code /order 200
check_code /api/account/me 401
check_code /api/account/security/events 401

# 后台功能仍为原后台，UI 仅主题化；鉴权边界必须保持不变。
check_code /admin/login 200
check_code /api/admin/site-config 401
check_code /api/admin/global-ui-settings 401
check_code /api/admin/frontend-controls 401
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
check_code /api/admin/ai-chat/status 401
exit "$fail"
