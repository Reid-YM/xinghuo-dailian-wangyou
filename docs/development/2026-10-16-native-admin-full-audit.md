# 新后台全量迁移审查与修复（2026-10-16）

本轮以当前源码全部旧 `/api/admin` 管理方法为清单，逐项对应新版 API、页面入口、权限、字段和保存行为。82 个旧接口均存在新版对应入口；60 个站点字段和 66 个导航/主题字段均有表单控件。接口映射不是实际员工验收证明；真实支付、企业微信写入和真机验收仍单独记录。

## 本轮修复

| 问题 | 修复及验证范围 |
| --- | --- |
| 前端控制台仍复用整个旧页面，并动态加载旧样式 | 独立新版中文外壳与样式，共享表单；旧默认入口跳转新版，显式 legacy=1 仅管理员可用 |
| 默认登录页仍用旧布局 | 原登录路由、账号密码及 CSRF 保持，改为新版中文布局，错误/退出提示及手机表单 |
| 工具编辑器从旧 admin.html 抽取且依赖 app.css | 独立共享片段，新版拥有完整工具组件样式 |
| 会员操作日志未迁移 | 新增会员日志数据源；audit:view 且全部数据范围，公开会员编号、操作、脱敏 IP、时间，不输出原始详情或 UA |
| CRM 意向/来源筛选遗漏 | 新表单、URL 状态、服务端精确筛选，保留部门/本人数据范围和总数 |
| 攻略元数据、FAQ、关联攻略不可编辑 | 新增完整编辑控件，沿用并发版本、草稿及权限保护 |
| 攻略保存覆盖关联商品自定义资料 | 保存保留已有标签、描述、图片、推荐标志及排序 |
| 站点三个后端可写字段没有表单 | 补齐服务状态、状态短文案、主按钮文案 |
| 前端配置重复提交与草稿保护不足 | 按表单防重复、分区草稿保护、冲突保留、读取失败禁写并可重试 |
| 中文会员角色显示含义错误 | 会员 ADMIN 显示“管理员”；后台恢复管理员仍单独标识 |
| 覆盖扫描漏掉方法级支付管理路径 | 扫描实际解析路径，纳入三个支付管理接口，新增接口未登记即测试失败 |

## 界面取舍

视觉延续新版深色画布、黄色主动作、明确表单分区。前台展示控制台和工具站各自拥有组件样式；共享表单结构避免新旧两套字段漂移。桌面采用双列表单，手机单列，控制台顶部动作换成两列且不固定占用屏幕。读取/保存/只读/错误/冲突有独立状态提示。

## 验证与边界

- 全量 Maven、权限/数据范围、日志脱敏、CRM SQL 筛选、默认入口授权、模板渲染和覆盖契约。
- 实际 Chromium：攻略完整字段与商品保留、CRM 筛选、会员日志切换、站点三个字段保存、前台控制分区保存/重试/冲突、工具保存/冲突、登录与手机/桌面布局。
- Linux CI 执行 Chromium/WebKit 完整浏览器集；最终结果在 PR 检查中记录。
- 无数据库迁移、无生产配置或密钥修改、未开启真实收款/目录写入；本轮修复需单独发布后才在生产生效。
- 支付旧 GET 最新记录在新版支付列表按业务编号查询，订单定位使用新版订单查询；创建/关闭仍受开关、确认金额、权限及版本保护。未发起真实交易。
- 显式旧版恢复页保留；正常工作台入口和本表中的功能无需跳回旧后台。

## 逐接口对应表

机器可校验清单：[`admin-migration-coverage.json`](../admin-migration-coverage.json)。此表登记对应关系；字段与行为由独立业务/浏览器测试验证，不把“路由存在”当成全部功能正确。

| 旧管理接口 | 新版接口 | 新版工作台 / 差异说明 |
| --- | --- | --- |
| `GET /api/admin/dashboard` | `GET /api/admin/xingwei/statistics` | `/admin/xingwei/statistics`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/site-config` | `GET /api/admin/xingwei/site/{area}` | `/admin/xingwei/site`；area=site；旧图片清空对应 visual 分区中清空对应图片字段。 |
| `PUT /api/admin/game-status/{gameCode}` | `PATCH /api/admin/xingwei/site/{area}/{section}` | `/admin/xingwei/site`；area=games，section 为游戏编号。 |
| `PATCH /api/admin/orders/{orderNo}/public-case` | `PATCH /api/admin/xingwei/maintenance/orders/{key}/case` | `/admin/xingwei/maintenance`；明确客户授权、脱敏摘要及当前版本。 |
| `PATCH /api/admin/site-config/section/{section}` | `PATCH /api/admin/xingwei/site/{area}/{section}` | `/admin/xingwei/site`；area=site；旧图片清空对应 visual 分区中清空对应图片字段。 |
| `PATCH /api/admin/site-config` | `PATCH /api/admin/xingwei/site/{area}` | `/admin/xingwei/site`；area=site；旧图片清空对应 visual 分区中清空对应图片字段。 |
| `POST /api/admin/site-config/image/{slot}` | `POST /api/admin/xingwei/site/images/{field}` | `/admin/xingwei/site`；旧 slot 对应新版图片 field，上传后保存引用。 |
| `DELETE /api/admin/site-config/image/{slot}` | `PATCH /api/admin/xingwei/site/{area}` | `/admin/xingwei/site`；area=site；旧图片清空对应 visual 分区中清空对应图片字段。 |
| `POST /api/admin/services/cover-image` | `POST /api/admin/xingwei/catalog-details/images/{kind}` | `/admin/xingwei/catalog`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/tool-station/image` | `POST /api/admin/xingwei/tool-station/image` | `/admin/xingwei/tool-station`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/catalog` | `GET /api/admin/xingwei/catalog-details` | `/admin/xingwei/catalog-details`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/catalog/games` | `POST /api/admin/xingwei/catalog/taxonomy/games` | `/admin/xingwei/taxonomy`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/catalog/categories` | `POST /api/admin/xingwei/catalog/taxonomy/categories` | `/admin/xingwei/taxonomy`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PUT /api/admin/catalog/games/{code}` | `PUT /api/admin/xingwei/catalog/taxonomy/games/{code}/profile`<br>`PUT /api/admin/xingwei/catalog/taxonomy/games/{code}/active` | `/admin/xingwei/taxonomy`；资料编辑与启停分离；启停前读取公开影响并明确确认。 |
| `PUT /api/admin/catalog/games/{code}/profile` | `PUT /api/admin/xingwei/catalog-details/games/{code}` | `/admin/xingwei/catalog-details`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PATCH /api/admin/catalog/games/bulk` | `POST /api/admin/xingwei/catalog-details/games/bulk` | `/admin/xingwei/catalog-details`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/catalog/games/image/{kind}` | `POST /api/admin/xingwei/catalog-details/images/{kind}` | `/admin/xingwei/catalog-details`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `DELETE /api/admin/catalog/games/{code}` | `DELETE /api/admin/xingwei/catalog-details/{kind}/{key}` | `/admin/xingwei/catalog-details`；kind=games/categories，必须无依赖并确认编号与版本。 |
| `PUT /api/admin/catalog/categories/{id}` | `PUT /api/admin/xingwei/catalog/taxonomy/categories/{id}/profile`<br>`PUT /api/admin/xingwei/catalog/taxonomy/categories/{id}/active` | `/admin/xingwei/taxonomy`；资料编辑与启停分离；启停前读取公开影响并明确确认。 |
| `DELETE /api/admin/catalog/categories/{id}` | `DELETE /api/admin/xingwei/catalog-details/{kind}/{key}` | `/admin/xingwei/catalog-details`；kind=games/categories，必须无依赖并确认编号与版本。 |
| `GET /api/admin/services` | `GET /api/admin/xingwei/catalog/snapshot` | `/admin/xingwei/catalog`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/services` | `POST /api/admin/xingwei/catalog/services` | `/admin/xingwei/catalog`；新建默认下线；发布检查和确认通过后上架，不能绕过检查。 |
| `PUT /api/admin/services/{id}` | `PUT /api/admin/xingwei/catalog/services/{id}` | `/admin/xingwei/catalog`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `DELETE /api/admin/services/{id}` | `DELETE /api/admin/xingwei/catalog/services/{id}` | `/admin/xingwei/catalog`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/tool-station` | `GET /api/admin/xingwei/tool-station` | `/admin/xingwei/tool-station`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PUT /api/admin/tool-station` | `PUT /api/admin/xingwei/tool-station` | `/admin/xingwei/tool-station`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/tool-station/reset` | `POST /api/admin/xingwei/tool-station/reset` | `/admin/xingwei/tool-station`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/services/{id}/restore` | `POST /api/admin/xingwei/catalog/services/{id}/restore` | `/admin/xingwei/catalog`；新建默认下线；发布检查和确认通过后上架，不能绕过检查。 |
| `GET /api/admin/quotes` | `GET /api/admin/xingwei/business/quotes` | `/admin/xingwei/business?tab=quotes`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PATCH /api/admin/quotes/{id}/status` | `PATCH /api/admin/xingwei/business/quotes/{no}/status` | `/admin/xingwei/business?tab=quotes`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `DELETE /api/admin/quotes/{id}` | `DELETE /api/admin/xingwei/maintenance/{type}/{key}` | `/admin/xingwei/maintenance`；type=quotes/orders；按业务编号定位，版本与依赖检查、管理员确认。 |
| `POST /api/admin/quotes/{id}/convert` | `POST /api/admin/xingwei/business/quotes/{no}/convert` | `/admin/xingwei/business?tab=quotes`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/quotes/{id}/image` | `GET /api/admin/xingwei/business/quotes/{no}/image` | `/admin/xingwei/business?tab=quotes`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/orders` | `GET /api/admin/xingwei/business/orders` | `/admin/xingwei/business?tab=orders`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/orders` | `POST /api/admin/xingwei/business/orders` | `/admin/xingwei/business?tab=orders`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PATCH /api/admin/orders/{orderNo}` | `PATCH /api/admin/xingwei/business/orders/{no}` | `/admin/xingwei/business?tab=orders`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/orders/{orderNo}/image` | `POST /api/admin/xingwei/business/orders/{no}/image` | `/admin/xingwei/business?tab=orders`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `DELETE /api/admin/orders/{orderNo}` | `DELETE /api/admin/xingwei/maintenance/{type}/{key}` | `/admin/xingwei/maintenance`；type=quotes/orders；按业务编号定位，版本与依赖检查、管理员确认。 |
| `GET /api/admin/slots` | `GET /api/admin/xingwei/operations/display` | `/admin/xingwei/operations?module=display`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PATCH /api/admin/slots` | `PATCH /api/admin/xingwei/operations/display` | `/admin/xingwei/operations?module=display`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/audits` | `GET /api/admin/xingwei/audits` | `/admin/xingwei/audits`；员工操作按数据范围查询，原始敏感内容不直接输出。 |
| `GET /api/admin/test-feedback` | `GET /api/admin/xingwei/operations/feedback` | `/admin/xingwei/operations?module=feedback`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/test-feedback` | `POST /api/admin/xingwei/operations/feedback` | `/admin/xingwei/operations?module=feedback`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PUT /api/admin/test-feedback/{id}` | `PATCH /api/admin/xingwei/operations/feedback/{id}` | `/admin/xingwei/operations?module=feedback`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/test-feedback/{id}/attachment` | `POST /api/admin/xingwei/operations/feedback/{id}/attachment` | `/admin/xingwei/operations?module=feedback`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/test-feedback/{id}/attachment` | `GET /api/admin/xingwei/operations/feedback/{id}/attachment` | `/admin/xingwei/operations?module=feedback`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/account/users` | `GET /api/admin/xingwei/operations/members` | `/admin/xingwei/operations?module=members`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/users` | `POST /api/admin/xingwei/operations/members` | `/admin/xingwei/operations?module=members`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/users/{id}` | `PATCH /api/admin/xingwei/operations/members/{id}` | `/admin/xingwei/operations?module=members`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/users/{id}/disable` | `PATCH /api/admin/xingwei/operations/members/{id}` | `/admin/xingwei/operations?module=members`；会员编辑状态设为 DISABLED；撤销旧会话。 |
| `DELETE /api/admin/account/users/{id}` | `DELETE /api/admin/xingwei/operations/members/{id}` | `/admin/xingwei/operations?module=members`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/users/{id}/email-verified` | `POST /api/admin/xingwei/operations/members/{id}/verified` | `/admin/xingwei/operations?module=members`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/users/{id}/reset-password` | `POST /api/admin/xingwei/operations/members/{id}/password` | `/admin/xingwei/operations?module=members`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/account/users/{id}/login-logs` | `GET /api/admin/xingwei/operations/members/{id}/logs` | `/admin/xingwei/operations?module=members`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/account/operation-logs` | `GET /api/admin/xingwei/audits/member-activity` | `/admin/xingwei/audits?source=members`；会员公开编号、操作类型、脱敏网络及时间；全站审计权限及全部数据范围；不输出原始敏感日志。 |
| `GET /api/admin/account/comments` | `GET /api/admin/xingwei/operations/comments` | `/admin/xingwei/operations?module=comments`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/comments/{id}/review` | `POST /api/admin/xingwei/operations/comments/{id}/review` | `/admin/xingwei/operations?module=comments`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `DELETE /api/admin/account/comments/{id}` | `DELETE /api/admin/xingwei/operations/comments/{id}` | `/admin/xingwei/operations?module=comments`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/account/submissions` | `GET /api/admin/xingwei/operations/submissions` | `/admin/xingwei/operations?module=submissions`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/account/submissions/{id}` | `GET /api/admin/xingwei/operations/submissions/{id}` | `/admin/xingwei/operations?module=submissions`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/submissions/{id}/review` | `POST /api/admin/xingwei/operations/submissions/{id}/review` | `/admin/xingwei/operations?module=submissions`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/account/submissions/{id}/publish` | `POST /api/admin/xingwei/operations/submissions/{id}/publish` | `/admin/xingwei/operations?module=submissions`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/guides` | `GET /api/admin/xingwei/guides` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/guides/{id}` | `GET /api/admin/xingwei/guides/{id}` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/guides` | `POST /api/admin/xingwei/guides` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PUT /api/admin/guides/{id}` | `PUT /api/admin/xingwei/guides/{id}` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `PATCH /api/admin/guides/{id}/status` | `PATCH /api/admin/xingwei/guides/{id}/status` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `DELETE /api/admin/guides/{id}` | `DELETE /api/admin/xingwei/guides/{id}` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `DELETE /api/admin/guides/batch` | `DELETE /api/admin/xingwei/guides/batch` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/guides/archive-all` | `POST /api/admin/xingwei/guides/archive-all` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `POST /api/admin/guides/import-games` | `POST /api/admin/xingwei/guides/import-games` | `/admin/xingwei/guides`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/leads` | `GET /api/admin/xingwei/operations/leads` | `/admin/xingwei/operations?module=crm`；按业务编号定位；保留关键词、状态、意向、来源筛选与业务数据范围。 |
| `PATCH /api/admin/leads/{id}/crm` | `PATCH /api/admin/xingwei/operations/leads/{no}` | `/admin/xingwei/operations?module=crm`；按业务编号定位；保留关键词、状态、意向、来源筛选与业务数据范围。 |
| `POST /api/admin/leads/{id}/ai-suggestion` | `POST /api/admin/xingwei/operations/leads/{no}/suggestion` | `/admin/xingwei/operations?module=crm`；按业务编号定位；保留关键词、状态、意向、来源筛选与业务数据范围。 |
| `GET /api/admin/tracking/summary` | `GET /api/admin/xingwei/statistics` | `/admin/xingwei/statistics`；沿用权限、版本校验和审计；在新版工作台完成操作。 |
| `GET /api/admin/frontend-controls` | `GET /api/admin/xingwei/site/{area}` | `/admin/xingwei/frontend-controls`；area 为 frontend 或 global-ui；同一配置源，独立新版外壳。 |
| `PATCH /api/admin/frontend-controls` | `PATCH /api/admin/xingwei/site/{area}` | `/admin/xingwei/frontend-controls`；area 为 frontend 或 global-ui；同一配置源，独立新版外壳。 |
| `GET /api/admin/global-ui-settings` | `GET /api/admin/xingwei/site/{area}` | `/admin/xingwei/frontend-controls`；area 为 frontend 或 global-ui；同一配置源，独立新版外壳。 |
| `PATCH /api/admin/global-ui-settings` | `PATCH /api/admin/xingwei/site/{area}` | `/admin/xingwei/frontend-controls`；area 为 frontend 或 global-ui；同一配置源，独立新版外壳。 |
| `POST /api/admin/orders/{orderNo}/wechat-payment` | `POST /api/admin/xingwei/payments/orders/{no}` | `/admin/xingwei/payments`；新版支付工作台按订单定位并按业务编号查询支付记录；真实收款仍受服务端开关、权限、金额确认及版本约束。 |
| `GET /api/admin/orders/{orderNo}/wechat-payment` | `GET /api/admin/xingwei/payments/orders/{no}`<br>`GET /api/admin/xingwei/payments` | `/admin/xingwei/payments`；新版支付工作台按订单定位并按业务编号查询支付记录；真实收款仍受服务端开关、权限、金额确认及版本约束。 |
| `POST /api/admin/wechat-payments/{paymentNo}/close` | `POST /api/admin/xingwei/payments/{no}/close` | `/admin/xingwei/payments`；新版支付工作台按订单定位并按业务编号查询支付记录；真实收款仍受服务端开关、权限、金额确认及版本约束。 |
