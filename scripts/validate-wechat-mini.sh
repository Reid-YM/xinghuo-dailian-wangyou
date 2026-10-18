#!/usr/bin/env bash
set -euo pipefail

root="${1:-wechat-mini}"
client="$root/miniprogram"

for file in "$root/project.config.json" "$client/app.js" "$client/app.json" \
  "$client/pages/me/me.js" "$client/pages/me/me.wxml" \
  "$client/utils/site-account.js" "$root/cloudfunctions/login/index.js" \
  "$root/cloudfunctions/syncProgress/index.js"; do
  [[ -f "$file" ]] || { echo "::error::Missing mini-program file: $file"; exit 1; }
done

node - "$root" <<'NODE'
const fs = require('fs');
const path = require('path');
const root = process.argv[2];
const project = JSON.parse(fs.readFileSync(path.join(root, 'project.config.json'), 'utf8'));
const app = JSON.parse(fs.readFileSync(path.join(root, 'miniprogram/app.json'), 'utf8'));
if (project.miniprogramRoot !== 'miniprogram/' || project.cloudfunctionRoot !== 'cloudfunctions/') {
  throw new Error('Mini-program root or cloud-function root changed');
}
if (!/^wx[0-9a-f]{16}$/i.test(project.appid)) throw new Error('Expected the collaborator mini-program AppID');
if (!app.pages.includes('pages/me/me')) throw new Error('Website account page is not registered');
const business = fs.readFileSync(path.join(root, 'miniprogram/config/business.js'), 'utf8');
if (!business.includes("base: 'https://wydlgf.cn'")) throw new Error('Website API origin changed');
const page = fs.readFileSync(path.join(root, 'miniprogram/pages/me/me.js'), 'utf8');
if (!page.includes('wx.scanCode') || !page.includes('xingwei-login:')) throw new Error('Website QR login flow is missing');
const account = fs.readFileSync(path.join(root, 'miniprogram/utils/site-account.js'), 'utf8');
if (!account.includes("'/api/account/wechat-mini/confirm'")) throw new Error('Website confirmation API is missing');
if (!account.includes('wx.login')) throw new Error('Fresh WeChat identity exchange is missing');
console.log('Mini-program project and website login contract OK');
NODE

if grep -RIlE 'WECHAT_MINI_APP_SECRET[[:space:]]*=|API_V3_KEY[[:space:]]*=|-----BEGIN PRIVATE KEY-----' \
  "$client" "$root/cloudfunctions"; then
  echo '::error::Client/cloud source contains a server secret.'
  exit 1
fi

node "$root/tools/validate.js"
node "$root/tools/simulate.js"
node "$root/tools/test-sync.js"
node "$root/tools/check-images.js"
node "$root/tools/test-site-account-page.js"
node "$root/tools/test-camo-guide.js"
echo 'PASS: collaborator mini-program source and website login validation succeeded.'
