# N23 攻略中心 Next 预览

## 目标

把仍停留在 Legacy 的公开攻略中心纳入 Frontend Next 预览链路，同时继续保留旧 `/guides` 作为明确回退与 SEO 主入口。

N23 不切换正式默认流量，不新增攻略写接口，不复制后台编辑能力。

## 新增路由

- `/next/guides`
- `/next/guides/`

攻略详情继续复用 N22：

- `/next/guides/{slug}`

## 数据来源

只读现有公开接口：

- `GET /api/guides?limit=100`

接口仍由 `PublicGuideController` 与 `GuideContentService.publicList` 提供，只返回公开攻略。

N23 不新增 `POST` / `PATCH` / `PUT` / `DELETE` 请求，也不引入第二套攻略数据。

## 页面能力

N23 在 Frontend Next 共享文档中新增攻略中心页面并提供：

- 已公开攻略列表；
- 标题、摘要、游戏、难度、预计阅读时间、精选标记与标签；
- `q` 关键词搜索；
- `game` 游戏筛选；
- 搜索和筛选状态写入 URL；
- 浏览器前进 / 后退恢复筛选状态；
- 攻略卡片进入 `/next/guides/{slug}`；
- 读取失败时显式错误和重试；
- 异步请求序号保护，避免旧响应覆盖新状态。

所有动态文本继续通过 `textContent` 等安全 DOM API 渲染，不使用任意 `innerHTML`。

## 路由连续性

N14 从 N23 起增加：

- 普通 `/guides` -> `/next/guides`；
- 普通 `/guides/{slug}` 继续 -> `/next/guides/{slug}`；
- 带 `legacy=1` 或 `data-n23-legacy="1"` 的链接保持 Legacy。

N23 也会在 Next 顶部导航中加入“攻略”入口。

## SEO 与回退

Next 攻略中心仍属于并行预览：

- canonical 指向 `https://wydlgf.cn/guides`；
- robots 为 `noindex,follow`；
- 明确保留“查看旧版攻略中心”链接 `/guides?legacy=1`。

因此 N23 不与正式公开攻略中心争夺索引，也不删除旧页面。

## 安全边界

- 正式默认前端仍由 `app.frontend-next.default-enabled=false` 控制；
- `/guides` 正式页面不被本阶段替换；
- 不修改订单、支付、退款、售后等业务写链路；
- 不使用 `localStorage` / `sessionStorage` 存储攻略状态；
- 不新增数据库 migration；
- 不改变后台攻略编辑权限。

## 后续

N24 建议继续处理个人资产中仍固定 Legacy 的投稿创建 / 编辑链路；随后推进工具站与 Mastery 等剩余公开旧版页面。