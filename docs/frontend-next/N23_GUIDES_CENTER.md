# N23 攻略中心 Next 预览

N23 将攻略中心从“详情已 Next 化、中心仍回旧版”的状态继续向前推进，新增显式 `/next/guides` 预览入口，并与 N22 `/next/guides/{slug}` 详情形成完整浏览闭环。

## 目标

- 新增 `/next/guides` 显式预览路由，不替换公开旧 `/guides`。
- 复用现有公开只读接口 `GET /api/guides?limit=100`，不新增后端业务 API。
- 提供搜索、游戏筛选和排序，并把 `q`、`game`、`sort` 保存到 URL，刷新和前进后退可恢复。
- 攻略卡片直接进入 `/next/guides/{slug}`，继续复用 N22 详情。
- Next 环境里的普通 `/guides` 链接由 N14 归一到 `/next/guides`；显式旧版入口继续保留。
- 列表预览 canonical 指回 `https://wydlgf.cn/guides`，并使用 `noindex,follow`，避免与正式旧攻略中心争夺索引。

## 数据与安全边界

N23 只读取现有公开攻略列表接口：

- `GET /api/guides?limit=100`

当前 `GuideSummary` 已提供游戏、标题、摘要、封面、难度、预计阅读时间、标签、精选状态、浏览量、发布时间、章节数等列表展示所需字段，因此 N23 不复制后台编辑逻辑，也不增加写请求。

运行时明确保持：

- 仅 `GET`；
- `credentials: same-origin`；
- `cache: no-store`；
- 过期响应保护；
- 不使用 `innerHTML` / `insertAdjacentHTML`；
- 不使用 `localStorage` / `sessionStorage`；
- 图片只接受站内绝对路径或 `https://`。

## 路由连续性

- `/guides` -> `/next/guides`（仅在 Frontend Next 内普通链接归一时）；
- `/guides/{slug}` -> `/next/guides/{slug}`；
- `/next/guides` 卡片 -> `/next/guides/{slug}`；
- N22 面包屑中的“攻略中心”现在可被 N14 归一回 `/next/guides`；
- `data-n23-legacy="1"` 保留明确旧版 `/guides` 对照入口；
- N22 的旧攻略详情逃生口仍保持 legacy，不被 N23 改写。

## 当前边界

N23 只完成公开攻略中心的 Next 浏览体验。攻略后台编辑、投稿编辑、工具站和 Mastery 仍按后续最终替换阶段推进。
