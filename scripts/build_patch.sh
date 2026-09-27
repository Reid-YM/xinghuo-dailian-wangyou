#!/usr/bin/env bash
set -euo pipefail
if [ "$#" -lt 3 ]; then
  echo "usage: build_patch.sh SOURCE_ROOT BASE_JAR OUTPUT_JAR" >&2
  exit 2
fi
SOURCE_ROOT=$(realpath "$1")
BASE_JAR=$(realpath "$2")
OUTPUT_JAR=$(realpath -m "$3")
WORK=$(mktemp -d /tmp/xinghuo-guide-build.XXXXXX)
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/classpath" "$WORK/classes" "$WORK/patch/BOOT-INF/classes"
unzip -q "$BASE_JAR" -d "$WORK/classpath"
cp -a "$SOURCE_ROOT/src/main/resources/." "$WORK/patch/BOOT-INF/classes/"
find "$SOURCE_ROOT/src/main/java" -type f -name '*.java' -print0 | sort -z | xargs -0 javac --release 21 -encoding UTF-8 -parameters -cp "$WORK/classpath/BOOT-INF/classes:$WORK/classpath/BOOT-INF/lib/*" -d "$WORK/classes"
cp -a "$WORK/classes/." "$WORK/patch/BOOT-INF/classes/"
cp -a "$BASE_JAR" "$OUTPUT_JAR"
jar --update --file "$OUTPUT_JAR" --no-compress -C "$WORK/patch" .
python3 "$SOURCE_ROOT/scripts/prune_jar.py" "$OUTPUT_JAR" "$SOURCE_ROOT/scripts/delete-jar-entries.txt"
if [[ "$(unzip -v "$OUTPUT_JAR" BOOT-INF/lib/spring-boot-3.5.5.jar)" != *Stored* ]]; then
  echo 'nested dependency compression check failed' >&2
  exit 3
fi
echo "built $OUTPUT_JAR"