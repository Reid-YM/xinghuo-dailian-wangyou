# 星维网络微信小程序（账号互通 + 微信支付）

这是与网站 `https://wydlgf.cn` 同仓库版本化的小程序客户端。客户端不使用云开发，不保存网站密码、AppSecret、APIv3 密钥、PEM 私钥或游戏账号信息。

## 导入微信开发者工具

1. 微信开发者工具选择“导入项目”，目录选择本仓库的 `wechat-mini/`。
2. `project.config.json` 中的 `touristappid` 是仓库安全占位。导入后只在开发者工具本地项目设置中选择已注册的“星维网络”小程序 AppID；不要把 AppSecret 写进客户端文件。
3. 小程序后台 `request` 合法域名需要包含 `https://wydlgf.cn`。
4. 当前不上传头像，因此不要求 `uploadFile` 合法域名。手机号授权由网站服务端调用微信接口完成。

## 网站登录流程

1. 用户在网站 `/account/login` 点击“使用星维网络小程序登录”。
2. 网站生成 5 分钟有效、一次性的 8 位登录码，并显示二维码；服务端只保存登录码哈希。
3. 用户在小程序点击“扫码登录网站”，二维码内容格式为 `xingwei-login:12345678`。
4. 小程序重新调用 `wx.login` 获取一次性 code 并提交 `/api/account/wechat-mini/confirm`。
5. 原浏览器再次确认后才绑定身份并签发网站会话。二维码截图或短码泄露不能单独领取网站会话。

## 小程序支付流程（V3H）

1. 客服先在网站后台为已绑定账号的订单确认最终服务名称与人民币金额。客户端不能创建金额。
2. 已关联微信在小程序里通过新的 `wx.login` code 读取自己的支付单。
3. 用户点击“微信支付”后，服务端校验当前 openid 与网站账号的直接绑定，并校验微信支付 AppID 与小程序 AppID 一致。
4. 服务端使用微信支付 API v3 JSAPI/小程序预下单，返回 `wx.requestPayment` 所需参数；小程序本身不参与签名。
5. `wx.requestPayment` 返回 success 只代表客户端流程结束，不直接把订单标记成已支付。最终状态只以微信支付验签通知或服务端主动查单为准。
6. 同一个客服支付单第一次预下单会锁定通道：网页 Native 二维码与小程序支付二选一。需要切换方式时，由客服先关闭旧支付单再重新建立，避免一个 `out_trade_no` 混用两种交易类型。

## 已接接口

- `POST /api/account/wechat-mini/confirm`：扫码/输入登录码后确认微信身份。
- `POST /api/account/wechat-mini/profile`：读取当前微信是否已关联网站账号。
- `POST /api/account/wechat-mini/sync-profile`：同步网站显示昵称。
- `POST /api/account/wechat-mini/sync-phone`：授权手机号后写入已关联账号联系方式。
- `POST /api/account/wechat-mini/payments`：以新的 `wx.login` code 读取当前账号的微信支付单。
- `POST /api/account/wechat-mini/payments/{paymentNo}/prepay`：为当前账号已有支付单申请小程序支付参数；金额与服务名不能由客户端传入。

## 发布前检查

- 网站生产环境已配置 `WECHAT_MINI_APP_ID` 与 `WECHAT_MINI_APP_SECRET`。
- 微信支付商户已把该小程序 AppID 与商户号建立合法绑定，且 `WECHAT_PAY_APP_ID` 与小程序 AppID 一致。
- 生产服务器公网 IP 已加入小程序后台 IP 白名单（手机号授权需要）。
- `https://wydlgf.cn` 已加入 request 合法域名。
- 先保持真实收款开关受控；支付联调使用负责人确认的测试订单与小额实付，不把 APIv3 密钥或 PEM 内容发到聊天/GitHub。
- 真机至少验证：首次关联、已关联登录、Native/小程序通道互斥、用户取消支付、重复点击、回调重复、主动查单恢复、支付参数过期。
