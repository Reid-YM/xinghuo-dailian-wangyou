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
check_code /loadouts 404
check_code /api/loadouts 404
check_code /assets/loadouts.js 404
check_code /api/admin/ai-chat/status 404
exit "$fail"