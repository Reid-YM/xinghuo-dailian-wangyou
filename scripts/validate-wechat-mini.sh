#!/usr/bin/env bash
set -euo pipefail

root="${1:-wechat-mini}"

required=(
  "$root/app.js"
  "$root/app.json"
  "$root/app.wxss"
  "$root/project.config.json"
  "$root/sitemap.json"
  "$root/pages/account/index.js"
  "$root/pages/account/index.json"
  "$root/pages/account/index.wxml"
  "$root/pages/account/index.wxss"
)

for file in "${required[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "::error::Missing WeChat mini-program file: $file"
    exit 1
  fi
done

echo "Node: $(node --version)"
node --check "$root/app.js"
node --check "$root/pages/account/index.js"

node - "$root" <<'NODE'
const fs = require('fs');
const path = require('path');
const root = process.argv[2];
const jsonFiles = [
  'app.json',
  'project.config.json',
  'sitemap.json',
  'pages/account/index.json'
];
for (const relative of jsonFiles) {
  const file = path.join(root, relative);
  JSON.parse(fs.readFileSync(file, 'utf8'));
  console.log(`JSON OK: ${relative}`);
}
const project = JSON.parse(fs.readFileSync(path.join(root, 'project.config.json'), 'utf8'));
if (project.appid !== 'touristappid') {
  throw new Error('project.config.json must keep touristappid; select the real AppID only in local WeChat DevTools settings.');
}
const app = fs.readFileSync(path.join(root, 'app.js'), 'utf8');
if (!app.includes("apiBase: 'https://wydlgf.cn'")) {
  throw new Error('app.js must use the production HTTPS API base https://wydlgf.cn');
}
NODE

# Client-side source must never contain server credentials or cloud-development calls.
if grep -RInE --exclude='README.md' --exclude='.gitignore' \
  'WECHAT_MINI_APP_SECRET|appSecret|app_secret|AppSecret|wx\.cloud|cloudfunctionsRoot' "$root"; then
  echo "::error::Forbidden server secret/cloud-development reference found in WeChat mini-program client source."
  exit 1
fi

# The scan-login contract should remain explicit and tied to the existing website API.
grep -Fq "xingwei-login:" "$root/pages/account/index.js" || {
  echo "::error::Missing xingwei-login QR payload parser."
  exit 1
}
grep -Fq "/api/account/wechat-mini/confirm" "$root/pages/account/index.js" || {
  echo "::error::Missing mini-program confirmation endpoint."
  exit 1
}
grep -Fq "wx.scanCode" "$root/pages/account/index.js" || {
  echo "::error::Missing wx.scanCode login flow."
  exit 1
}
grep -Fq "wx.login" "$root/pages/account/index.js" || {
  echo "::error::Missing wx.login identity exchange flow."
  exit 1
}

echo "PASS: WeChat mini-program source validation succeeded."
