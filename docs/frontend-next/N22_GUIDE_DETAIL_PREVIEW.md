# N22 攻略详情 Next 预览连续性

## 目标

N22 收口 N21 个人收藏中仍会跳回旧版的攻略详情链接，为公开攻略详情增加独立 Frontend Next 预览路由：

- `/next/guides/{slug}`

本阶段只读取已有公开攻略接口，不创建第二套攻略数据，也不改变旧版 `/guides/{slug}`。

## 数据来源

N22 直接读取现有公开接口：

- `GET /api/guides/{slug}`

接口仍由 `PublicGuideController` 与 `GuideContentService.publicDetail` 提供，只返回已公开攻略。N22 不新增后端业务 API，不复制后台攻略编辑逻辑。

详情页展示已有字段：

- 攻略标题、副标题、摘要、封面；
- 游戏、难度、预计阅读时间、作者、标签；
- 正文章节和章节图片；
- 关联服务；
- FAQ；
- 相关攻略。

正文的 `bodyMarkdown` 不通过 `innerHTML` 注入，而是以安全 DOM 文本节点渲染基础段落、列表与标题，避免把后台内容直接当 HTML 执行。

## 路由连续性

`n14-route-continuity.js` 现在识别：

- `/guides/{slug}` → `/next/guides/{slug}`

但显式旧版入口继续保留：

- `data-n22-legacy="1"`；
- 或 `legacy=1`。

因此 N22 页面中的“攻略中心（旧版）”与“旧版攻略详情”不会被 N14 再次归一。

N21 的收藏区域不再把 `/guides/{slug}` 标记成 legacy。收藏里的 GUIDE 链接在 N14 观察到后会进入 N22；`/mastery` 与 `/account/submit` 仍保持旧版，因为它们尚未有 Next 对应页。

## 页面行为

N22 runtime：

`src/main/resources/static/next/n22-guide-detail.js`

行为包括：

- 仅在 `/next/guides/{slug}` 加载详情；
- 使用请求序号忽略过期响应，避免快速导航时旧响应覆盖新页面；
- 读取失败显示明确错误与重试；
- 关联服务进入 `/next/service/{slug}`；
- 服务询价进入 `/next/quote?service={slug}`；
- 相关攻略继续进入 `/next/guides/{slug}`；
- 浏览器前进/后退会重新同步当前攻略；
- canonical 始终指向原公开 `/guides/{slug}`；
- Next 预览详情使用 `noindex,follow`，避免预览路径与公开旧版详情形成重复索引。

## 后端与安全边界

N22：

- 不新增数据库迁移；
- 不新增写 API；
- 不执行 POST / PUT / PATCH / DELETE；
- 不使用 localStorage / sessionStorage；
- 不改变攻略发布状态；
- 不改变收藏、投稿或账号写入语义；
- 不开启支付、退款或任何外部写操作。

## 代码范围

新增：

- `src/main/resources/static/next/n22-guide-detail.js`
- `src/test/java/com/xinghuo/web/FrontendNextN22GuideDetailContractTest.java`

调整：

- `FrontendNextPreviewController`：增加 `/next/guides/{slug}`、N22 runtime 注入与内容指纹；
- `n14-route-continuity.js`：攻略详情进入 Next，并识别 N22 显式 legacy；
- `n21-account-personal-assets.js`：攻略详情交给 N14/N22，不再固定旧版；
- N21 契约：同步新的攻略链接边界。

## 验证边界

仓库已加入静态契约，锁定路由、公开 GET、无业务写请求、安全 DOM 渲染、相关链接和过期响应保护。

当前对话直接通过 GitHub connector 修改开发分支，没有本地工作区，因此不能把“测试文件存在”表述为 Maven / 浏览器测试已实际执行通过。是否通过 CI 以分支真实检查结果为准。

## 发布状态

N22 继续遵守 Frontend Next 开发边界：

- 不部署；
- 不合并 `main`；
- 不切正式流量；
- Legacy 继续作为正式前端；
- `app.frontend-next.default-enabled=false` 不变。

## 2026-10-19 完整迁移补查

旧攻略页还有收藏与讨论能力，仅提供只读正文不能算完整迁移。Next 详情现在建立讨论区域并显式调用共享 `XHGuideCommunity.mount(root)`，继续由既有 `assets/guide-community.js` 唯一拥有收藏、评论、回复、举报及账号请求协议。保留 `X-Requested-With: fetch`、原审核状态、已删除内容过滤与安全文本渲染；登录返回保留具体 Next 攻略地址。评论失败保留输入，提交期间阻止重复提交和编辑，评论列表失败可原位重试。N22 的正文读取本身继续只用原公开攻略 GET。

`FrontendNextN30GuideCommunityPlaywrightTest` 覆盖 Next 整页实际加载、收藏、举报、回复提交失败/重试、待审显示、删除过滤、安全文本及访客回跳；开发验证不等于生产提交评论或真实员工审核验收。社区 JS/CSS 与阶段脚本均由服务器按实际内容产生版本。

## 2026-10-19 全站整合更新

普通工具站、撰写投稿和攻略中心链接已进入当前 Next/正式命名空间，不再因早期阶段的旧版标记降级。明确注明“旧版”的入口仍保留 legacy=1。当前路由、分页/返回与鉴权验证以 COMPLETION_AUDIT.md 为准。
