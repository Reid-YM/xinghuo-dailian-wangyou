# Frontend Next 迁移状态

N1–N8 已完成视觉、真实服务/询价、账号和业务、安全与统一认证及并行联调。早期 README 中的“N2 当前阶段”已经过时；各阶段详细记录继续保留作历史证据。

N9 已在开发分支完成“服务入口灰度控制”首版：只为公共服务入口增加 `legacy` / `next` 双态、安全白名单、后台审计和快速回退能力。详见 [N9 服务入口灰度控制](N9_SERVICE_ENTRY_ROLLOUT.md)。

N10 已继续补齐“服务详情预览链路”：新增 `/next/service/{slug}`，Next 内部服务卡片保持在新版预览体验中，详情只读现有公开目录数据，并可带服务参数进入 `/next/quote?service=...`。详情读取增加过期响应保护和明确重试；旧 `/service/{slug}` 继续原样保留。详见 [N10 服务详情预览链路](N10_SERVICE_DETAIL_PREVIEW.md)。

N11 已补充“相关服务连续浏览”：N10 详情下方按真实公开目录展示同游戏其他服务，最多 3 项，全部继续进入 `/next/service/{slug}`；推荐模块失败不影响主详情。详见 [N11 相关服务连续浏览](N11_RELATED_SERVICES_CONTINUITY.md)。

N12 已补齐“询价上下文连续性”：`/next/quote?service={slug}` 会明确展示当前服务、恢复游戏/分类/服务选择，并在手动更换服务时用 `replaceState` 同步 `service` 参数；其它 UTM 查询参数继续保留，同时可直接返回对应 `/next/service/{slug}`。详见 [N12 询价上下文连续性](N12_QUOTE_CONTEXT_CONTINUITY.md)。

N13 已补齐“业务记录预览链路”：N3 询价提交成功后进入 `/next/record/{businessNo}`，只读现有公开记录与进度截图，保留旧 `/record/{businessNo}` 对照入口，并对记录预览设置 `noindex,nofollow`。详见 [N13 业务记录预览链路](N13_RECORD_DETAIL_PREVIEW.md)。

**N9–N13 当前均只在开发分支开发，不部署、不合并、不切正式流量。**

用户要求正式前端恢复旧版。当前代码默认关闭 Next 正式映射，发布后以实际正式页面核验为准；新版后台继续保留。详见 [前端回退记录](../development/2026-10-16-frontend-rollback.md)。重新启用正式 Next 必须单独获得明确授权。

- 默认入口 /、/services、/quote、/account 及认证/设置子页恢复旧视图和原账号鉴权。
- /next、/next/services、/next/service/{slug}、/next/quote、/next/record/{businessNo}、/next/account 保留为显式预览入口；旧正式入口继续存在。
- 页面级回退使用 ?legacy=1；整体回退配置 app.frontend-next.default-enabled=false。不能通过改静态首页文件假称已切换实际路由。
- JS 由真实内容指纹确定缓存版本。账号/console 写请求遵循服务端 X-Requested-With: fetch，不放宽鉴权。
- 数据失败时只保留已读真实数据或错误/重试状态，不再回退模拟商品、价格或订单。
- 原业务 API、数据库、金额确认与 Session/CSRF 保持；不引入第二套业务数据，不启用真实收款或企业微信写入。

辅助账号功能与后台模块仍按完整迁移清单推进；当前不能宣称完整替换已完成。
