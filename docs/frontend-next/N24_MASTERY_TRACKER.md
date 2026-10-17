# N24 MW4 工具站 Next 预览

N24 将旧 `/mastery` 的核心能力迁入 Frontend Next，新增显式 `/next/mastery`，重点不是复制静态页面，而是保持用户已有进度数据连续。

## 目标

- 新增 `/next/mastery` 显式预览入口，不替换公开旧 `/mastery`。
- 继续读取现有公开 `GET /api/tool-station`，不新增工具站业务 API。
- 沿用旧工具站本机存档键 `xinghuo-mw4-camo-tracker-v2`，旧版与新版读取同一份本机进度。
- 沿用账号云同步工具键 `mw4_camo_tracker` 与 `/api/account/progress/{toolKey}` 乐观锁协议。
- 支持模式切换、总体/模式进度、武器搜索、类型筛选、完成状态、收藏、迷彩逐项勾选和非武器模式全局项。
- 沿用 `xinghuo-cod-tool-quote-snapshot`，可把当前进度摘要带入后续询价流程。
- Next 环境普通 `/mastery` 由 N14 归一到 `/next/mastery`；显式旧版入口使用 `data-n24-legacy="1"` 保留。

## 数据连续性

N24 与旧 `/mastery` 共用：

- `localStorage`: `xinghuo-mw4-camo-tracker-v2`
- cloud tool key: `mw4_camo_tracker`
- quote snapshot: `xinghuo-cod-tool-quote-snapshot`

因此用户在旧版已经勾选的进度与收藏可在新版直接读取；新版保存后回到旧版也继续读取相同结构。

登录账号后继续使用：

- `GET /api/account/me`
- `GET /api/account/progress/mw4_camo_tracker`
- `PUT /api/account/progress/mw4_camo_tracker`

写请求继续携带 `X-Requested-With: fetch`，并保留 `version` 乐观锁。遇到 `409` 时先读取云端最新版，避免静默覆盖其它设备进度。

## 页面行为

- URL 支持 `mode`、`q`、`type`、`status`，刷新和浏览器历史可恢复工具视图。
- 当前模式为武器迷彩时展示迷彩层级、武器列表和逐项完成状态。
- 非武器模式按全局项展示，不伪造武器维度。
- 修改后先保存本机，再延迟同步账号；账号接口失败不影响本机进度。
- 登录失效或云端冲突都有明确状态，不静默丢数据。

## 路由连续性

N21 过去会把个人资产区的 `/mastery` 标记为 legacy。N24 不删除该兼容标记，而是在 N14 中只对 `data-n21-legacy="1"` 且目标精确为 `/mastery` 的链接解除 legacy，让个人资产中的工具入口进入 `/next/mastery`。

`/account/submit` 等其它 N21 legacy 目标仍保持旧版，不被本阶段放开。

## SEO 与回退

- canonical: `https://wydlgf.cn/mastery`
- robots: `noindex,follow`
- 旧 `/mastery` 始终保留，并在 N24 页面提供显式对照入口。

## 后续

N24 完成工具站核心替换后，下一优先项是投稿创建/编辑 Next 化，然后再处理剩余辅助页与全站统一验收。
