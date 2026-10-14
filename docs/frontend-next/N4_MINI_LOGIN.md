# Frontend Next N4 补强：小程序扫码登录网站

## 目标

把项目现有的“星火迷彩追踪器小程序扫码登录网站”桥接能力接入 `/next/account`，同时保留 N4 已有的账号密码登录和微信开放平台网页扫码登录。

## 复用的现有后端

- `GET /api/account/wechat-mini/availability`
- `POST /api/account/wechat-mini/challenge`
- `POST /api/account/wechat-mini/poll`
- `POST /api/account/wechat-mini/complete`

不新增第二套微信身份、不在前端存储小程序登录凭据。

## 用户流程

1. 匿名用户进入 `/next/account`。
2. 页面检查小程序扫码登录是否已配置。
3. 点击“生成小程序登录二维码”。
4. 服务器生成一次性 8 位码、二维码和浏览器侧 HttpOnly 配对 Cookie，有效期 5 分钟。
5. 用户使用“星火迷彩追踪器”小程序扫码或输入 8 位码并确认。
6. 网站轮询只获取状态；状态 ready 后展示微信识别码尾号提示，要求用户核对。
7. 用户点击完成登录，网站调用 `/complete`，由服务端给当前浏览器签发正式账号会话。
8. 浏览器回到 `/next/account`，N4 读取 `/api/account/me` 和当前账号数据。

## 安全 / 降级

- 配对秘密只在 HttpOnly Cookie 中，不暴露给页面 JS。
- 二维码只接受后端返回的 `data:image/png;base64,...` 数据源。
- 二维码渲染失败时，8 位码仍可完成同一流程。
- 页面不会要求输入微信密码、短信验证码、access token 或 openid。
- 小程序能力未配置时，按钮禁用；账号密码登录、微信开放平台扫码和旧 `/account` 继续可用。
- 动态 DOM 使用 `textContent` / DOM API，不使用 `innerHTML`。

## 回退

删除 `n4-mini.js` runtime 即可移除 Frontend Next 的小程序入口，不影响：

- 旧账号中心的小程序扫码登录；
- N4 账号密码登录；
- 微信开放平台扫码登录；
- N3 询价；
- 新版后台。
