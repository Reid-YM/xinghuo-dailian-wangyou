# Frontend Next 迁移状态

N1–N8 已完成视觉、真实服务/询价、账号和业务、安全与统一认证及并行联调。早期 README 中的“N2 当前阶段”已经过时；各阶段详细记录继续保留作历史证据。

N9 已完成“服务入口灰度控制”首版：只为公共服务入口增加 `legacy` / `next` 双态、安全白名单、后台审计和快速回退能力。详见 [N9 服务入口灰度控制](N9_SERVICE_ENTRY_ROLLOUT.md)。

N10 已继续补齐“服务详情预览链路”：新增 `/next/service/{slug}`，Next 内部服务卡片保持在新版预览体验中，详情只读现有公开目录数据，并可带服务参数进入 `/next/quote?service=...`。详情读取增加过期响应保护和明确重试；旧 `/service/{slug}` 继续原样保留。详见 [N10 服务详情预览链路](N10_SERVICE_DETAIL_PREVIEW.md)。

N11 已补充“相关服务连续浏览”：N10 详情下方按真实公开目录展示同游戏其他服务，最多 3 项，全部继续进入 `/next/service/{slug}`；推荐模块失败不影响主详情。详见 [N11 相关服务连续浏览](N11_RELATED_SERVICES_CONTINUITY.md)。

N12 已补齐“询价上下文连续性”：`/next/quote?service={slug}` 会明确展示当前服务、恢复游戏/分类/服务选择，并在手动更换服务时用 `replaceState` 同步 `service` 参数；其它 UTM 查询参数继续保留，同时可直接返回对应 `/next/service/{slug}`。详见 [N12 询价上下文连续性](N12_QUOTE_CONTEXT_CONTINUITY.md)。

N13 已补齐“业务记录预览链路”：N3 询价提交成功后进入 `/next/record/{businessNo}`，只读现有公开记录与进度截图，保留旧 `/record/{businessNo}` 对照入口，并对记录预览设置 `noindex,nofollow`。详见 [N13 业务记录预览链路](N13_RECORD_DETAIL_PREVIEW.md)。

N14 已补齐“Next 内部路由连续性”：Next 页面里的普通 `/services`、`/quote`、`/account`、`/service/{slug}`、`/record/{businessNo}` 链接统一留在 `/next` 路径族；首页游戏卡片使用已加载真实 catalog 的 `seoSlug -> code` 映射进入 `/next/services?game={code}`。带 `legacy=1` 和已有显式 legacy 标记的旧版入口继续保留。N22 起攻略详情 `/guides/{slug}` 纳入归一链路，N23 起攻略中心 `/guides` 也归一到 `/next/guides`。详见 [N14 Next 内部路由连续性](N14_ROUTE_CONTINUITY.md)。

N15 已补齐“认证回跳连续性”：密码登录、注册、微信开放平台扫码和小程序扫码统一使用安全 `next` 返回目标；已有旧版 `/services`、`/quote`、`/account`、`/service/{slug}`、`/record/{businessNo}` 深链在认证完成后归一到对应 `/next` 路径。微信 OAuth 取消、限流或失败时，只有从 Next 发起的流程返回 `/next/account`，旧版登录流程继续保持旧版。详见 [N15 认证回跳连续性](N15_AUTH_RETURN_CONTINUITY.md)。

N16 已补齐“个人中心子路由深链闭环”：`requests`、`orders`、`progress`、`settings` 及已有兼容别名都可通过显式 `/next/account/...` 预览；N5 订单/询价 tab 会同步 URL，浏览器前进后退会恢复对应上下文，设置锚点定位到 N6/N4 的真实资料、联系方式、安全、设备和安全活动模块。认证 `next` 参数中的账号子路由也会继续留在 Next。详见 [N16 个人中心子路由深链闭环](N16_ACCOUNT_DEEP_LINK_CONTINUITY.md)。

N17 已增加“个人中心内部导航”：登录后的 Next 账号区提供总览、询价记录、订单记录、工具存档、账号设置五个 canonical 入口，复用 N16 深链与 N4/N5/N6 真实模块；切换使用浏览器历史，不新增业务 API，访客登录页不展示业务导航。详见 [N17 个人中心内部导航](N17_ACCOUNT_NAVIGATION.md)。

N18 已补齐“账号业务详情深链”：新增 `/next/account/requests/{businessNo}` 与 `/next/account/orders/{businessNo}`，直接复用 N5 既有询价/订单详情和原 `/api/console/...` 请求，不新增 fetch；详情打开、关闭、刷新、前进后退和未登录认证回跳都保持具体业务编号上下文，同时 N17 继续高亮对应父导航。详见 [N18 个人中心业务详情深链](N18_ACCOUNT_DETAIL_DEEP_LINKS.md)。

N19 已补齐“账号详情上下文连续性”：详情内可在当前已加载列表中上一条/下一条、返回询价或订单列表；订单仅在来源询价已存在于当前账号已加载列表时提供“来源询价”。详情还可进入 N13 公开业务记录，并通过严格的 `from=account-requests|account-orders` 枚举返回原账号详情；N19 不新增 fetch 或业务写请求。详见 [N19 账号详情上下文连续性](N19_ACCOUNT_DETAIL_CONTEXT.md)。

N20 已补齐“账号列表视图状态连续性”：在 N5 已真实加载的数据范围内支持 `q` 搜索、真实状态筛选和更新时间排序，并将视图状态保存在 URL；N19 的上一条/下一条按筛选后结果顺序工作，进入 N13 公开业务记录再返回时继续恢复白名单视图参数。当前 `/api/console/quotes` 与 `/orders` 仍由服务端固定返回第 0 页最多 50 条，因此 N20 明确只做已加载记录过滤，不伪装服务端分页，也不新增 fetch。详见 [N20 账号列表视图状态连续性](N20_ACCOUNT_LIST_VIEW_STATE.md)。

N21 已补齐“个人资产路由连续性”：新增 `/next/account/favorites` 与 `/next/account/submissions` 显式预览路由，并在 N5 现有个人资产区增加工具存档、收藏、投稿子导航。收藏/投稿的 `page` URL 状态通过现有 N5 上一页/下一页驱动原真实分页请求恢复，不新增 fetch；服务收藏继续交给 N14 进入 Next 服务详情，游戏收藏只有在真实 catalog 可解析时进入 Next 服务大厅。N22 起攻略收藏也可继续留在 Next；工具站与投稿编辑仍保留旧版入口。详见 [N21 个人资产路由连续性](N21_ACCOUNT_PERSONAL_ASSETS.md)。

N22 已补齐“攻略详情 Next 预览连续性”：新增 `/next/guides/{slug}`，只读现有 `GET /api/guides/{slug}`，安全渲染标题、章节、配图、相关服务、FAQ 与相关攻略；N14 将普通攻略详情链接归一到 Next，N21 收藏中的 GUIDE 不再被固定为 legacy。旧 `/guides/{slug}` 继续作为明确对照入口，Next 预览详情使用 canonical 指回公开旧版 URL 并设置 `noindex,follow`。详见 [N22 攻略详情 Next 预览连续性](N22_GUIDE_DETAIL_PREVIEW.md)。

N23 已进入“最终替换阶段”的第一步：新增 `/next/guides` 攻略中心，直接复用现有 `GET /api/guides?limit=100`，提供关键词搜索、游戏筛选、URL 状态、浏览器历史恢复、错误重试和到 N22 详情的连续浏览；顶部 Next 导航新增“攻略”入口。旧 `/guides` 继续作为 SEO 主入口和明确回退页面，Next 中心使用 canonical 指回旧公开 URL 并设置 `noindex,follow`。详见 [N23 攻略中心 Next 预览](N23_GUIDE_CENTER_PREVIEW.md)。

## 当前部署状态

N9–N22 已通过 PR #530 合并到 `main`，并按标准生产发布链路部署到生产应用；正式默认前端仍保持 Legacy，`/next/**` 作为显式并行预览入口。N23 当前在 `frontend-next-final-replacement-n23-20261017` 开发分支继续推进，尚未合并或部署。

用户要求正式前端保持旧版。当前代码默认关闭 Next 正式映射；重新启用正式 Next 必须单独获得明确授权。详见 [前端回退记录](../development/2026-10-16-frontend-rollback.md)。

- 默认入口 `/`、`/services`、`/quote`、`/account` 及认证/设置子页继续使用旧视图和原账号鉴权。
- `/next`、`/next/services`、`/next/service/{slug}`、`/next/guides`、`/next/guides/{slug}`、`/next/quote`、`/next/record/{businessNo}`、`/next/account` 及受支持的 `/next/account/...` 子路由作为显式预览入口；旧正式入口继续存在。
- 页面级回退使用 `?legacy=1`；整体回退配置 `app.frontend-next.default-enabled=false`。不能通过改静态首页文件假称已切换实际路由。
- JS 由真实内容指纹确定缓存版本。账号/console 写请求遵循服务端 `X-Requested-With: fetch`，不放宽鉴权。
- 数据失败时只保留已读真实数据或错误/重试状态，不回退模拟商品、价格或订单。
- 原业务 API、数据库、金额确认与 Session/CSRF 保持；不引入第二套业务数据，不启用真实收款或企业微信写入。

## 最终替换阶段建议顺序

- N23：攻略中心 Next 化（当前开发）。
- N24：投稿创建 / 编辑 Next 化。
- N25：工具站与工具资产公开页 Next 化。
- N26：Mastery / 辅助公开页 Next 化。
- N27：剩余账号辅助入口与跨页连续性收口。
- N28：全站视觉、加载、空状态、错误状态、移动端一致性收口。
- N29：生产 `/next/**` 全链路真实验收与缺陷清零。
- N30：正式切换前治理、灰度计划、回滚演练与切换清单；是否切正式流量仍需单独授权。

当前仍不能宣称 Frontend Next 已完整替代 Legacy；目标是在 N23–N30 完成剩余约 20% 的替换与验收工作。
