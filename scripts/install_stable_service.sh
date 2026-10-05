#!/usr/bin/env bash
set -euo pipefail

SERVICE=${SERVICE:-xinghuo-v2.service}
APP_DIR=${APP_DIR:-/opt/xinghuo-v2}
APP_LINK=${APP_LINK:-$APP_DIR/current.jar}
SERVICE_FILE=${SERVICE_FILE:-/etc/systemd/system/$SERVICE}
JAVA_BIN=${JAVA_BIN:-/usr/bin/java}

sudo install -d -o root -g root -m 0755 "$APP_DIR" "$APP_DIR/releases" "$APP_DIR/backups"

if [ ! -e "$APP_LINK" ] && [ -f "$APP_DIR/xinghuo-v2-1.0.0.jar" ]; then
  sudo ln -sfn "$APP_DIR/xinghuo-v2-1.0.0.jar" "$APP_LINK"
fi

sudo tee "$SERVICE_FILE" >/dev/null <<EOF
[Unit]
Description=Xinghuo V2 Spring Boot
After=network.target mysql.service

[Service]
Type=simple
User=xinghuo
Group=xinghuo
WorkingDirectory=$APP_DIR
EnvironmentFile=$APP_DIR/.env
ExecStart=$JAVA_BIN -Xms256m -Xmx768m -jar $APP_LINK
Restart=always
RestartSec=5
SuccessExitStatus=143
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ReadWritePaths=$APP_DIR/uploads $APP_DIR/logs

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE" >/dev/null
echo "installed $SERVICE_FILE using $APP_LINK"
