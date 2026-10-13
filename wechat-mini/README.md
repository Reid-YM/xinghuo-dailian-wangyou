# 星维网络微信小程序（网站扫码登录）

这是与网站 `https://wydlgf.cn` 同仓库版本化的小程序客户端。当前第一阶段只做账号互通，不使用云开发，不把网站密码、AppSecret、微信服务端凭据或游戏账号信息放进小程序。

## 导入微信开发者工具

1. 微信开发者工具选择“导入项目”，目录选择本仓库的 `wechat-mini/`。
2. `project.config.json` 中使用 `touristappid` 只是仓库安全占位。导入后在开发者工具的本地项目设置中选择已经注册的“星维网络”小程序 AppID；不要把 AppSecret 写进任何客户端文件。
3. 小程序后台 `request` 合法域名需要包含 `https://wydlgf.cn`。
4. 当前版本不上传头像，因此不要求 `uploadFile` 合法域名。手机号授权由网站服务端调用微信接口完成。

## 登录流程

1. 用户在网站 `/account/login` 点击“使用星维网络小程序登录”。
2. 网站生成 5 分钟有效、一次性的 8 位登录码，同时把同一个码编码为二维码；服务端只保存登录码哈希。
3. 用户在小程序点击“扫码登录网站”，二维码内容格式为 `xingwei-login:12345678`。
4. 小程序重新调用 `wx.login` 获取一次性 code，并提交到 `/api/account/wechat-mini/confirm`。
5. 网站原浏览器轮询到 ready 后显示微信识别码尾号，用户在原浏览器再次点击确认，才会绑定账号并签发网站会话。

二维码被截屏或登录码泄露都不能单独领取网站会话，因为最后一步必须持有原浏览器的 HttpOnly 高熵 Cookie。

## 已接接口

- `POST /api/account/wechat-mini/confirm`：扫码/输入登录码后确认微信身份。
- `POST /api/account/wechat-mini/profile`：读取当前微信是否已关联网站账号。
- `POST /api/account/wechat-mini/sync-profile`：同步网站显示昵称。
- `POST /api/account/wechat-mini/sync-phone`：授权手机号后写入已关联账号联系方式。

所有请求都会在调用前重新执行一次 `wx.login`，不会复用已经消费的一次性微信 code。

## 发布前检查

- 网站生产环境已配置 `WECHAT_MINI_APP_ID` 与 `WECHAT_MINI_APP_SECRET`。
- 生产服务器公网 IP 已加入微信小程序后台 IP 白名单（手机号授权需要）。
- `https://wydlgf.cn` 已加入 request 合法域名。
- 用两台设备验证：新微信首次登录、已关联微信登录、错误码、过期码、重复扫码、二维码截图后二次确认保护。
