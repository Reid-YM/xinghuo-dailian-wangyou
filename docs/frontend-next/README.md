# Frontend Next 迁移状态

N1–N8 已完成视觉、真实服务/询价、账号和业务、安全与统一认证及并行联调。早期 README 中的“N2 当前阶段”已经过时；各阶段详细记录继续保留作历史证据。

当前继续完整迁移替换。正式首页、服务、询价及账号核心视图已进入默认映射实现，只有本轮开发/CI/最终发布完成后才能宣称线上已替换。详见 docs/development/2026-10-16-complete-migration-plan.md 与 2026-10-16-default-frontend-replacement.md。

- 入口路径 /、/services、/quote、/account 及认证/设置子页使用新文档；原账号鉴权在视图转发之前执行。
- /next、/next/services、/next/quote、/next/account 继续作为别名。路径和旧 hash 均正确恢复页面。
- 页面级回退使用 ?legacy=1；整体回退配置 app.frontend-next.default-enabled=false。不能通过改静态首页文件假称已切换实际路由。
- JS 由真实内容指纹确定缓存版本。账号/console 写请求遵循服务端 X-Requested-With: fetch，不放宽鉴权。
- 数据失败时只保留已读真实数据或错误/重试状态，不再回退模拟商品、价格或订单。
- 原业务 API、数据库、金额确认与 Session/CSRF 保持；不引入第二套业务数据，不启用真实收款或企业微信写入。

辅助账号功能与后台模块仍按完整迁移清单推进；当前不能宣称完整替换已完成。
