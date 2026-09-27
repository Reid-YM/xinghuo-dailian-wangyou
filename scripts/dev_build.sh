#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BASE_JAR=${BASE_JAR:-/srv/xinghuo-dev/base/xinghuo-v2-base.jar}
OUTPUT=${1:-$ROOT/build/xinghuo-v2-dev.jar}
if [ ! -f "$BASE_JAR" ]; then
  echo "baseline jar not found: $BASE_JAR" >&2
  exit 2
fi
mkdir -p "$(dirname "$OUTPUT")"
exec bash "$ROOT/scripts/build_patch.sh" "$ROOT" "$BASE_JAR" "$OUTPUT"