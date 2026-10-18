# N25 MW4 Mastery 工具 Next 预览

## 目标

把目前最大的公开 Legacy 工具入口 `/mastery` 纳入 Frontend Next，同时避免复制 MW4 迷彩追踪器的业务逻辑与账号云同步协议。

N25 新增：

- `/next/mastery`
- `/next/mastery/`

正式 `/mastery` 继续作为公开 canonical 与回退入口。

## 核心策略：复用权威工具运行时

MW4 工具当前已经有成熟运行时：

- `/assets/mw4-camo-tracker.js`
- `/assets/mw4-camo-tracker.css`

该运行时已经负责：

- `GET /api/tool-station` 真实工具数据；
- 多人 / 战区 / DMZ / Pyroclast 模式；
- 武器搜索、类型筛选、完成状态筛选与收藏；
- 本机 `localStorage` 进度；
- 询价进度 snapshot；
- `GET/PUT /api/account/progress/mw4_camo_tracker` 账号云同步；
- `X-Requested-With: fetch` 写请求安全头；
- 乐观锁版本冲突处理。

N25 不复制上述状态机和 API 逻辑。`n25-mastery-tool.js` 只负责：

1. 在 Frontend Next 共享页面中构建原运行时要求的 DOM 挂载点；
2. 加载现有 CSS；
3. DOM 就绪后加载同一份 `mw4-camo-tracker.js`；
4. 把 Next 内的询价、登录和 `/mastery` 入口保持在 Next 路径族；
5. 提供 Legacy 对照/客服逃生入口。

因此 Legacy 与 Next 使用同一份本机存储 key、同一份云端 toolKey 和同一套工具数据，不产生进度分叉。

## 路由连续性

N14 从 N25 起增加：

- `/mastery` -> `/next/mastery`；
- `/mastery/` -> `/next/mastery`；
- `legacy=1` 或 `data-n25-legacy="1"` 保持旧版。

N21 此前把工具入口固定为 Legacy。N25 在 Next 环境检测到该入口后会改为 `/next/mastery` 并清除旧 N14/N21 legacy 标记，确保后续个人资产导航可进入 N25。

## 认证连续性

工具运行时未登录时原本会给出 `/account/login`。N25 会把同步登录动作保持为：

`/next/account?next=/next/mastery`

因此登录完成后可以回到 Next 工具页，不掉回旧版。

## 询价连续性

“带着进度询价”直接使用：

`/next/quote?toolProgress=1&gameCode=CODMW4`

现有工具运行时仍写入原 `QUOTE_KEY` snapshot，N3/N12 继续消费同一份上下文，不建立第二套工具询价数据。

## SEO / 回退

- canonical：`https://wydlgf.cn/mastery`；
- robots：`noindex,follow`；
- 正式 `/mastery` 不删除；
- `app.frontend-next.default-enabled=false` 不变。

## 安全边界

- 不新增工具数据 API；
- 不新增数据库 migration；
- 不改账号 progress 乐观锁协议；
- 不修改支付、订单、退款、售后；
- 不复制 MW4 武器/迷彩数据到 Next；
- 本机存储沿用原工具行为，以保证旧页和 Next 页看到同一份本机进度。

## 后续

N26 继续收口其它辅助公开页面与工具入口；完成后进入剩余账号跨页连续性和全站一致性阶段。