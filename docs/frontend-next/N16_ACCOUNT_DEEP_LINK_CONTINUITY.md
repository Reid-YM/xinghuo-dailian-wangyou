# N16 · 个人中心子路由深链闭环

## 目标

N16 继续收敛 Frontend Next 个人中心的导航连续性。旧个人中心已经拆分出需求、订单、工具存档和账号设置等真实子路由；此前 `/next/account` 只有一个入口，直接访问这些子路由时会丢失目标语义。

本阶段不新增第二套账号页面，也不新增业务 API。它只把现有 N4/N5/N6 模块与真实账号信息架构对齐。

## 显式预览路由

### Canonical

- `/next/account/requests`：询价/需求记录
- `/next/account/orders`：订单记录
- `/next/account/progress`：云端工具存档
- `/next/account/settings`：账号设置与安全

### 兼容别名

- `quotes`、`demands` → `requests`
- `order`、`order-list` → `orders`
- `tools`、`tool-progress` → `progress`
- `profile`、`settings/profile` → `settings#profile`
- `contact`、`settings/contact` → `settings#contact`
- `security`、`settings/security` → `settings#security`
- `devices` → `settings#devices`
- `activity` → `settings#activity`

这些别名只用于保持已有账号信息架构兼容；进入 Next 后会用 `replaceState` 归一到 canonical 路由，不产生额外后端页面。

## 路由与现有模块对应

- `requests` 使用 N5 的 `quotes` tab 和现有 `/api/console/quotes` 等真实账号接口。
- `orders` 使用 N5 的 `orders` tab 和现有账号订单接口。
- `progress` 定位 N5 已存在的 `data-personal-asset="progress"` 工具存档模块，继续读取 `/api/account/progress`。
- `settings#profile` 定位 N6 资料表单。
- `settings#contact` 定位 N6 回访联系方式表单。
- `settings#security` 定位 N6 密码/安全区域。
- `settings#devices` 定位 N6 登录设备区域。
- `settings#activity` 定位 N4 已存在的“最近安全活动”。

## 浏览器历史连续性

N5 的“我的订单 / 询价记录”按钮此前只切换 DOM，不更新 URL。N16 在不改 N5 数据逻辑的前提下补充：

- 询价 tab → `/next/account/requests`
- 订单 tab → `/next/account/orders`
- `popstate` 恢复对应 tab
- settings fragment 的 `hashchange` 恢复对应定位

因此刷新、复制链接、浏览器前进后退都能保留个人中心上下文。

## 登录回跳

N15 已统一密码、注册、微信 OAuth、小程序扫码的 `next` 回跳。N16 进一步把受支持的旧账号子路由归一到 `/next/account/...`，例如：

- `next=/account/requests` → `next=/next/account/requests`
- `next=/account/settings#profile` → `next=/next/account/settings#profile`

带 `legacy=1` 的明确旧版目标不会被改写。

## 未来正式迁移语义

`FrontendMigrationConfig` 仍默认关闭：

`app.frontend-next.default-enabled=false`

N16 只把未来可能启用的 canonical 映射从统一 `/next/account` 改为对应子路由：

- `/account/requests` → `/next/account/requests`
- `/account/orders` → `/next/account/orders`
- `/account/progress` → `/next/account/progress`
- `/account/settings` → `/next/account/settings`

原 handler 和账号鉴权仍先执行，`?legacy=1` 仍跳过替换。

## 安全边界

`n16-account-deep-links.js`：

- 不调用 `fetch`
- 不写业务 API
- 不使用 `localStorage` / `sessionStorage`
- 不使用 `innerHTML` / `insertAdjacentHTML`
- 不改变 Session、CSRF、支付、退款或管理员权限
- 仅处理同源、受支持的账号子路由
- 明确保留 `legacy=1`

## 验证

新增静态契约：

`FrontendNextN16AccountDeepLinkContinuityContractTest`

覆盖 Preview 路由、运行时注入和指纹、canonical/alias 映射、N5 tab URL、浏览器历史、N6/N4 定位、N15 `next` 连续性、正式迁移语义和只读导航安全边界。

本阶段仍为开发分支代码，不代表 Maven / Playwright 已执行通过，也不代表已部署或切换正式流量。
