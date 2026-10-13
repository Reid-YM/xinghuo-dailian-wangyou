#!/usr/bin/env bash
set -euo pipefail

controller="src/main/java/com/xinghuo/controller/XingweiAdminController.java"
dashboard="src/main/resources/templates/admin-xingwei.html"
releases="src/main/resources/templates/admin-xingwei-releases.html"
workflow=".github/workflows/ci.yml"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

require_file() {
  [ -f "$1" ] || fail "missing required file: $1"
}

require_literal() {
  local file="$1"
  local text="$2"
  grep -Fq -- "$text" "$file" || fail "$file is missing required text: $text"
}

for file in "$controller" "$dashboard" "$releases" "$workflow"; do
  require_file "$file"
done

extract_constant() {
  local name="$1"
  local matches
  matches=$(grep -E "^[[:space:]]*static final String ${name} = \"[^\"]+\";" "$controller" || true)
  [ "$(printf '%s\n' "$matches" | sed '/^$/d' | wc -l)" -eq 1 ] || fail "${name} must be declared exactly once"
  printf '%s\n' "$matches" | sed -E 's/^[^\"]*\"([^\"]+)\";.*/\1/'
}

console_version=$(extract_constant CONSOLE_VERSION)
console_stage=$(extract_constant CONSOLE_STAGE)

[[ "$console_version" =~ ^[0-9]{8}-v[0-9]+[a-z]$ ]] || fail "unexpected CONSOLE_VERSION format: $console_version"
[[ "$console_stage" =~ ^V[0-9]+[A-Z][[:space:]]·[[:space:]].+ ]] || fail "unexpected CONSOLE_STAGE format: $console_stage"

version_tag=$(printf '%s' "${console_version##*-}" | tr '[:lower:]' '[:upper:]')
stage_tag=${console_stage%% *}
[ "$version_tag" = "$stage_tag" ] || fail "version/stage mismatch: $console_version vs $console_stage"

# Rendered fallbacks must match the controller's version truth.
require_literal "$dashboard" "th:text=\"\${consoleStage}\">${console_stage}</p>"
require_literal "$dashboard" "th:text=\"\${consoleVersion}\">${console_version}</strong>"
require_literal "$releases" "th:text=\"\${consoleVersion}\">${console_version}</span>"

# The current stage must have a release-history entry. Stable production means the entry is live.
if [[ "$console_stage" == *"STABLE PRODUCTION"* ]]; then
  grep -Eq "new ReleaseItem\(\"[0-9]{4}-[0-9]{2}-[0-9]{2}\", \"${stage_tag}\", \"已上线\"," "$controller" \
    || fail "stable production stage ${stage_tag} must have an 已上线 release item"

  if grep -Fq 'RELEASE CANDIDATE' "$dashboard"; then
    fail "stable production dashboard must not contain RELEASE CANDIDATE"
  fi
  if grep -Fq '作为本次发布候选' "$dashboard"; then
    fail "stable production dashboard must not describe itself as a release candidate"
  fi
fi

# Known delivery truth that must not regress while real-device validation is pending.
require_literal "$controller" '"V3H", "待真机验收"'
require_literal "$dashboard" 'V3H 待真机验收'
require_literal "$dashboard" '生产流水线已启用'

if grep -Fq '<article class="xw-module xw-module-planned" data-search="微信支付' "$dashboard"; then
  fail "V3H payment is implemented/pending validation and must not be shown as merely planned"
fi
if grep -Fq '<article class="xw-module xw-module-planned" data-search="发布 部署' "$dashboard"; then
  fail "production deployment pipeline is active and must not be shown as planned"
fi

# Keep the update-record mechanism itself wired into CI.
require_literal "$workflow" 'bash -n scripts/deploy-zero-downtime.sh scripts/deploy-from-github.sh scripts/github-deploy-gateway.sh scripts/install-github-deploy-bridge.sh scripts/smoke_test.sh scripts/validate-wechat-mini.sh scripts/validate-xingwei-release-state.sh'
require_literal "$workflow" 'bash scripts/validate-xingwei-release-state.sh'

printf 'PASS: Xingwei release state is consistent (%s / %s).\n' "$console_version" "$console_stage"
