# V4J 售后待跟进时效分布

日期：2026-10-15

状态：候选版本已通过生产门禁，正在进行 Stable Production 状态收口。

## 目标

在 V4I 已上线的售后状态分布、问题类型分布与客户补充待跟进能力基础上，增加只读的“当前需跟进”时效分布，帮助客服识别长时间未响应的售后记录，同时不扩大业务写入面。

## 统计定义

“需跟进”只包含：

- 当前状态为 `OPEN`、尚未开始处理的售后记录；或
- 存在客户补充 ID 高于 `handled_through_followup_id` 的记录，即客服尚未确认处理到该条客户补充。

时长起点：

- 有未确认客户补充时，使用最新客户补充时间；
- 否则对 `OPEN` 记录使用原始售后提交时间。

分桶：

- 24 小时内；
- 24–72 小时；
- 72 小时以上。

三个时间桶互斥，合计等于当前需跟进数量。

## 已完成

- 新增只读 `XingweiAfterSalesAttentionAgeSummaryService`。
- 新增 `GET /api/admin/xingwei/business/after-sales/cases/attention-age-summary`。
- 权限继续使用 `order:view`。
- 数据范围继续使用 ALL / DEPARTMENT / SELF / DENIED。
- 后台售后概览增加当前需跟进、24 小时内、24–72 小时、72 小时以上四个只读卡片。
- 时效接口异常只影响时效卡片，不影响现有售后队列、状态分布和问题类型分布。
- 员工保存售后处理结果后自动刷新时效分布。
- 动态内容继续使用 `textContent`，不引入 `innerHTML`。
- 功能 PR #476 已通过 Java/Maven、版本真值守卫和 Chromium/WebKit 双 CI，并 squash 合入 `main`。
- 功能合并主线 SHA：`d9341f646806efb3f1833abf32d0eb7fc7ffd577`。
- 统一候选 PR #477 已通过双 CI 并合入，候选主线 SHA：`ebadafea77e22137de581307ec09f4aec06caf7d`。
- 精确候选 main SHA 的主线 Java/Maven、版本真值守卫与 Chromium/WebKit 双 CI 已全部通过。
- Deploy Production #205 已成功完成受控生产发布。
- 服务器 Maven：453 tests，0 failures，0 errors，82 skipped。
- 18080 预发布完整冒烟：通过。
- 8081 金丝雀完整冒烟：通过。
- Nginx 金丝雀切流：通过。
- 8080 正式完整冒烟：通过。
- Nginx 最终恢复到 8080；8081 金丝雀已停止。
- 候选生产 JAR SHA256：`e4fead9ec237f3f05d564aef87f9524a7a2de97dc947b509713322c2b4f1e2bc`。
- 当前候选生产 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261015153522.jar`。
- 上一版 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261015150731.jar`，继续保留用于回滚。
- 状态收口分支将版本真值从 `V4J · RELEASE CANDIDATE / 待发布` 同步为 `V4J · STABLE PRODUCTION / 已上线`，版本号及售后资产缓存继续保持 `20261015-v4j / 20261015v4j`。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 无订单状态写入变更。
- 无售后状态写入逻辑变更。
- 无退款、真实收款、微信支付或企业微信写入变更。
- V3H 小程序支付与企业微信写入继续独立验收。
- Stable 状态收口只修改版本真值、页面说明、契约测试与开发记录，不修改 V4J 业务实现。
- 旧后台与上一版 production release 均继续保留，可回滚。

## 测试覆盖

- 服务层：SELF / DEPARTMENT / ALL / DENIED 数据范围。
- 服务层：仅 OPEN 或未确认客户补充进入需跟进集合。
- 服务层：24 小时内 / 24–72 小时 / 72 小时以上互斥分桶。
- 服务层：缺少 `order:view` 时拒绝访问。
- 控制器：四个稳定 JSON 字段。
- Chromium：四个时效数字渲染、统计定义说明、现有筛选和 URL 不被时效概览读取修改。
- 统一版本：CONSOLE_VERSION / CONSOLE_STAGE / BUSINESS_VERSION、更新记录、后台首页、售后资产缓存、运行版本漂移与 RBAC 契约。
- 候选生产：服务器 Maven 与 18080 → 8081 → 8080 完整生产门禁。

## 下一步

1. 状态-only Stable Production PR 通过 Java/Maven、版本真值守卫及 Chromium/WebKit 双 CI。
2. 合入后对精确 stable main SHA 再跑主线双 CI。
3. 仅更新 `deploy/production` 的 `.deploy-request` 指向 stable main SHA。
4. 再执行一次 18080 → 8081 → 8080 状态-only 生产发布，使生产页面、`/api/admin/xingwei/health` 和仓库真值完全一致。
5. 发布后再次记录最终 JAR SHA256、current/previous release 与回滚目标，V4J 才完成最终稳定收口。
