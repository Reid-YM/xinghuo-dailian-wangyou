# N15 · 认证回跳连续性（仅开发）

## 目标

N14 已经把 Frontend Next 内部普通链接尽量保持在 `/next` 预览链路，但账号认证仍存在一个跨页面断点：密码登录和注册会读取 `next`，微信开放平台扫码与小程序扫码却固定回 `/next/account`；微信 OAuth 取消、限流或失败时，后端还会固定回旧版 `/account/login`。

N15 的目标是让同一次认证流程在成功和失败两种情况下都保留来源上下文，同时不改变旧版账号体系和既有 Session / OAuth / 小程序会话协议。

## 本阶段改动

### 1. 新增统一回跳运行时

新增：

- `src/main/resources/static/next/n15-auth-return.js`

职责：

- 覆盖 `window.XHFrontendShell.safeAccountReturn`，让 N4 密码登录和 N7 注册继续复用同一个安全返回规则。
- 只接受站内相对地址，拒绝外部 origin、`//`、反斜杠、CR/LF、`/admin`、`/api`、`/logout` 和认证回环地址。
- 对已有 Next 等价页做连续性归一：
  - `/`、`/home` → `/next`
  - `/services` → `/next/services`
  - `/quote` → `/next/quote`
  - `/account` → `/next/account`
  - `/service/{slug}` → `/next/service/{slug}`
  - `/record/{businessNo}` → `/next/record/{businessNo}`
- `legacy=1` 明确回退继续保持原地址，不强制归一。
- 普通 `/account/login`、`/account/register`、`/account/reset` 链接在 Next 中改为 `/next/account` 的认证模式，不影响带 `legacy=1` 的旧版入口。

### 2. 微信开放平台扫码登录

N4 原逻辑固定：

`/api/account/wechat/start?next=/next/account`

N15 在捕获阶段接管按钮，改为使用当前安全 `next` 目标发起 OAuth。

后端 `AccountWeChatController` 同时增加条件化错误返回：

- 如果 OAuth flow 的安全返回地址属于 `/next`，取消、限流、账号停用、配置异常和普通失败都返回 `/next/account?wechat=...&next=...`。
- 如果 flow 来自旧版，则仍返回原来的 `/account/login?wechat=...`。
- OAuth 成功路径仍使用原有 `flow.returnTo()`，Session、state、HttpOnly Cookie 和微信登录服务均不改变。

N15 前端会在 `#n4LoginMessage` 中显示对应微信失败状态，不需要跳回旧版页面才能看到错误原因。

### 3. 小程序扫码登录

N4 Mini 原逻辑在 `/api/account/wechat-mini/complete` 成功后固定跳 `/next/account`。

N15 在完成按钮的捕获阶段复用同一完成 API，成功后改为：

`location.assign(requestedReturn())`

挑战码、轮询、二维码、服务端会话领取协议都保持不变，仅修正最终返回目标。

## 安全边界

- 不新增账号体系。
- 不修改密码登录、注册、小程序 challenge/poll 的服务端协议。
- 不引入 `localStorage` 或 `sessionStorage` 保存返回地址。
- 不允许认证回跳进入 `/admin`、`/api`、`/logout`。
- 不允许外部 URL、scheme-relative URL 或反斜杠路径。
- 显式 `legacy=1` 继续可回退旧版。
- N15 唯一新增的客户端写请求是复用既有 `/api/account/wechat-mini/complete` 登录完成请求，不涉及订单、支付、退款或后台业务写入。

## 契约测试

新增：

- `FrontendNextN15AuthReturnContinuityContractTest`

覆盖：

- Preview Controller 注入及指纹。
- 已有旧路由到 Next 的安全归一。
- 密码登录、注册、微信 OAuth、小程序扫码共享同一 `next` 语义。
- 微信 OAuth 只有来源于 Next 时才把失败返回到 Next。
- 显式旧版入口保留。
- 无客户端持久化与非认证业务写入。

本阶段仍为开发分支静态契约开发，不宣称 Maven / Playwright 已执行通过。

## 发布状态

**仅开发，不部署、不合并 `main`、不切正式流量。**
