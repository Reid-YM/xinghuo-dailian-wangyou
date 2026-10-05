#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BASE_JAR=${BASE_JAR:-/srv/xinghuo-dev/base/xinghuo-v2-base.jar}
CFR_JAR=${CFR_JAR:-/srv/xinghuo-dev/tools/cfr.jar}
OUTPUT=${1:-$ROOT/reference/generated}
if [ ! -f "$CFR_JAR" ]; then
  echo "CFR jar not found: $CFR_JAR" >&2
  exit 2
fi
rm -rf "$OUTPUT"
mkdir -p "$OUTPUT"
java -jar "$CFR_JAR" "$BASE_JAR" --outputdir "$OUTPUT" --silent true
echo "decompiled reference written to $OUTPUT"