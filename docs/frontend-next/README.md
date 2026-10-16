# Frontend Next 迁移状态

N1–N8 已完成视觉、真实服务/询价、账号和业务、安全与统一认证及并行联调。早期 README 中的“N2 当前阶段”已经过时；各阶段详细记录继续保留作历史证据。

N9 已在开发分支进入“服务入口灰度控制”阶段：只为公共服务入口增加 `legacy` / `next` 双态、安全白名单、后台审计和快速回退能力。**当前仅开发，不部署、不合并、不切正式流量。** 详见 [N9 服务入口灰度控制](N9_SERVICE_ENTRY_ROLLOUT.md)。

用户要求正式前端恢复旧版。当前代码默认关闭 Next 正式映射，发布后以实际正式页面核验为准；新版后台继续保留。详见 [前端回退记录](../development/2026-10-16-frontend-rollback.md)。重新启用正式 Next 必须单独获得明确授权。

- 默认入口 /、/services、/quote、/account 及认证/设置子页恢复旧视图和原账号鉴权。
- /next、/next/services、/next/quote、/next/account 保留为显式预览入口。路径和旧 hash 均正确恢复页面。
- 页面级回退使用 ?legacy=1；整体回退配置 app.frontend-next.default-enabled=false。不能通过改静态首页文件假称已切换实际路由。
- JS 由真实内容指纹确定缓存版本。账号/console 写请求遵循服务端 X-Requested-With: fetch，不放宽鉴权。
- 数据失败时只保留已读真实数据或错误/重试状态，不再回退模拟商品、价格或订单。
- 原业务 API、数据库、金额确认与 Session/CSRF 保持；不引入第二套业务数据，不启用真实收款或企业微信写入。

辅助账号功能与后台模块仍按完整迁移清单推进；当前不能宣称完整替换已完成。
