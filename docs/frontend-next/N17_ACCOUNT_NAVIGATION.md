# N17 · 个人中心内部导航

## 目标

N16 已让个人中心子路由可以直接访问、刷新和前进后退，但登录后的 Next 个人中心仍缺少一套统一的新版子导航。N17 在不新增业务 API、不复制旧版页面的前提下，为现有 N4/N5/N6 模块增加可见的内部导航。

## 导航项

登录状态下显示：

- 总览 → `/next/account`
- 询价记录 → `/next/account/requests`
- 订单记录 → `/next/account/orders`
- 工具存档 → `/next/account/progress`
- 账号设置 → `/next/account/settings`

未登录时 `#n4LoginForm` 存在，N17 不挂载业务导航，避免访客误以为已经进入账号数据区。

## 路由状态

N17 复用 N16 的 canonical 路由：

- `quotes` / `demands` 仍高亮“询价记录”
- `order` / `order-list` 仍高亮“订单记录”
- `tools` / `tool-progress` 仍高亮“工具存档”
- `profile` / `contact` / `security` / `devices` / `activity` 与 `settings/*` 仍高亮“账号设置”

当前项使用 `aria-current="page"`，导航本身使用 `aria-label="个人中心导航"`。

## 无刷新导航

N17 的内部链接使用 `history.pushState`，随后调用 N16 的 `applyRoute()`：

- 询价 / 订单切换继续复用 N5 tab
- 工具存档继续定位 N5 的真实云端存档区
- 设置继续定位 N6/N4 的真实账号安全区
- 总览回到 N4 账号顶部
- 浏览器 `popstate` 会恢复导航高亮

因此账号区内部不需要为了切换工作区反复整页刷新。

## 生命周期

N4 会在登录、退出和账号状态刷新时重建账号 DOM。N17 使用 `MutationObserver`：

- 登录后的 `.n4-shell` 出现时挂载导航
- 登录表单出现或账号 shell 消失时移除导航
- N4 重绘后自动恢复导航

## 安全边界

`n17-account-navigation.js`：

- 不调用 `fetch`
- 不调用任何业务写 API
- 不使用 `localStorage` / `sessionStorage`
- 不使用 `innerHTML` / `insertAdjacentHTML`
- 只导航到 `/next/account` 路径族
- 不改变 Session、CSRF、支付、退款、账号权限或管理员权限

## 验证

新增静态契约：

`FrontendNextN17AccountNavigationContractTest`

覆盖运行时注入/指纹、canonical 导航、兼容别名高亮、访客隐藏、浏览器历史和只读导航边界。

本阶段仍只在开发分支，不代表 Maven / Playwright 已执行通过，也不代表已部署、合并 `main` 或切换正式流量。
