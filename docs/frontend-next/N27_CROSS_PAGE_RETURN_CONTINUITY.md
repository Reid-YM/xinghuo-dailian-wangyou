# N27 跨页认证返回连续性

## 目标

N23–N26 已把攻略、投稿、Mastery、业务记录入口、联系页和信任页纳入 Frontend Next。N27 解决剩余的跨页断点：用户从这些新版页面触发账号登录、注册或微信扫码时，`next` 返回目标不能再次落回 Legacy。

N27 不新增账号业务页面，也不修改认证 API，而是在现有 N15 安全返回规则外增加一层已迁移路由归一。

## 归一范围

以下旧公开目标在 Next 认证上下文中归一为对应 `/next/**`：

- `/` / `/home` -> `/next`
- `/services` -> `/next/services`
- `/quote` -> `/next/quote`
- `/guides` -> `/next/guides`
- `/guides/{slug}` -> `/next/guides/{slug}`
- `/mastery` -> `/next/mastery`
- `/records` -> `/next/records`
- `/record/{businessNo}` -> `/next/record/{businessNo}`
- `/contact` -> `/next/contact`
- `/trust` -> `/next/trust`
- `/service/{slug}` -> `/next/service/{slug}`
- `/account` -> `/next/account`
- `/account/submit` -> `/next/account/submit`

账号已迁移的子路由也继续保留在 Next，包括询价、订单、工具存档、收藏、投稿、设置、安全、设备、活动，以及询价/订单业务详情深链。

## 与 N15 的关系

N15 仍然是账号返回安全规则的最终裁决者，继续负责：

- 只允许同源站内路径；
- 拒绝 `//`、反斜杠、CR/LF 等异常目标；
- 拒绝 `/admin`、`/api`、`/logout` 等危险返回目标；
- 微信开放平台与小程序登录完成后的安全返回。

N27 不绕过 N15，而是：

1. 先把已知 Legacy 目标变成对应 Next 目标；
2. 再交给现有 `safeAccountReturn` 做最终安全判断；
3. 同步归一当前页面的 `next=` 查询参数；
4. 对后来动态生成的登录/注册/重置/微信开始链接中的嵌套 `next=` 做同样处理。

## 显式 Legacy

任何返回目标只要带 `legacy=1`，N27 就不进行 Next 归一，继续尊重明确回退意图。

## 安全边界

N27：

- 不发起任何 fetch；
- 不引入 POST / PUT / PATCH / DELETE；
- 不使用 localStorage / sessionStorage；
- 不修改账号 Session、OAuth state 或小程序票据；
- 不放宽 N15 的安全返回规则；
- 不新增数据库 migration。

## 后续

N28 进入全站一致性收口：统一新版各阶段叠加后产生的加载、空状态、错误、按钮、移动端和可访问性表现，并清理阶段性 UI 债务。