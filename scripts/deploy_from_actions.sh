#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
SERVICE=${SERVICE:-xinghuo-v2.service}
APP_DIR=${APP_DIR:-/opt/xinghuo-v2}
APP_JAR=${APP_JAR:-$APP_DIR/xinghuo-v2-1.0.0.jar}
BASE_JAR=${BASE_JAR:-/srv/xinghuo-dev/base/xinghuo-v2-base.jar}
RELEASES_DIR=${RELEASES_DIR:-$APP_DIR/releases}
BACKUPS_DIR=${BACKUPS_DIR:-$APP_DIR/backups}
HEALTH_URL=${HEALTH_URL:-}
TS=$(date +%Y%m%d%H%M%S)
BUILD_JAR="$ROOT/build/xinghuo-v2-$TS.jar"
RELEASE_JAR="$RELEASES_DIR/xinghuo-v2-$TS.jar"
BACKUP_JAR="$BACKUPS_DIR/xinghuo-v2-1.0.0.jar.$TS.bak"

if [ ! -f "$BASE_JAR" ]; then
  echo "baseline jar not found: $BASE_JAR" >&2
  exit 2
fi

BASE_JAR="$BASE_JAR" "$ROOT/scripts/dev_build.sh" "$BUILD_JAR"

sudo install -d -o root -g root -m 0755 "$RELEASES_DIR" "$BACKUPS_DIR"
if [ -f "$APP_JAR" ]; then
  sudo cp -a "$APP_JAR" "$BACKUP_JAR"
fi
sudo install -o xinghuo -g xinghuo -m 0640 "$BUILD_JAR" "$RELEASE_JAR"
sudo install -o xinghuo -g xinghuo -m 0640 "$BUILD_JAR" "$APP_JAR"
sudo systemctl restart "$SERVICE"
sleep 8
sudo systemctl is-active --quiet "$SERVICE"

if [ -n "$HEALTH_URL" ]; then
  curl -fsS --max-time 15 "$HEALTH_URL" >/dev/null
fi

echo "deployed $RELEASE_JAR"
if [ -f "$BACKUP_JAR" ]; then
  echo "backup $BACKUP_JAR"
fi
