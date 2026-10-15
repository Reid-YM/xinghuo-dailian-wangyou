# V4K 售后待跟进时效快捷队列

日期：2026-10-16

状态：已稳定上线生产。

## 目标

V4J 已上线当前需跟进总数与 24 小时内、24–72 小时、72 小时以上三档时效分布。V4K 将这组只读运营数字进一步变成可操作的只读队列，客服可以从售后概览直接进入对应时效范围，并继续组合处理状态、问题类型、客户补充和关键词条件。

## 本阶段实现

- 新增 `XingweiAfterSalesAttentionQueueService`。
- 新增只读接口：`GET /api/admin/xingwei/business/after-sales/cases/attention-queue`。
- 支持四种时效范围：
  - `ATTENTION`：全部当前需跟进；
  - `WITHIN_24H`：24 小时内；
  - `FROM_24_TO_72H`：24–72 小时；
  - `OVER_72H`：72 小时以上。
- “当前需跟进”定义与 V4J 汇总完全一致：
  - 有尚未确认的客户补充时，以最新补充时间作为待响应时间；
  - 否则仅 OPEN 记录以原始售后提交时间作为待响应时间；
  - 普通 IN_PROGRESS / RESOLVED 且没有新补充的记录不进入待跟进队列。
- 队列继续受 `order:view` 和 ALL / DEPARTMENT / SELF / DENIED 数据范围约束。
- 新增独立页面 `/admin/xingwei/after-sales/attention`，不替换原 `/admin/xingwei/after-sales/cases`。
- V4J 售后概览下方增加四个快捷入口，点击时带上当前 status / category / followup / q 条件并清零页码。
- 独立时效队列以 URL 作为筛选真值，支持刷新、前进/后退和分页。
- 时效队列为只读；需要处理具体售后时跳回原 V4J 处理页。

## 合并、CI 与生产证据

- 功能 PR #481 已通过 Java/Maven、版本真值守卫以及 Chromium + WebKit 全量交互回归；首次浏览器失败仅来自新增测试夹具缺少 HTML Content-Type，产品代码未改，测试夹具修正后第二轮双 CI 全绿。
- PR #481 已 squash 合入 `main`，功能主线 SHA：`b827aca2dab5cfcf8e23c426676a752a62de3445`。
- 精确功能主线 CI #1143 已通过 Java/Maven、版本真值守卫和 Chromium + WebKit 双 CI。
- 统一候选 PR #482 已通过双 CI 并合入；候选主线 SHA：`c2112185dc6760b1dbf22e6fa30806c6d2d85151`。
- 精确候选 main CI #1145 已通过 Java/Maven、版本真值守卫与 Chromium + WebKit 双 CI。
- Deploy Production #209 已成功完成候选生产发布：服务器 Maven 471 tests / 0 failures / 0 errors / 88 skipped，18080、8081、8080 完整冒烟均通过，Nginx 最终恢复 8080，8081 已停止。
- 候选生产 JAR SHA256：`a9f0f437e93d7b8577b44a41e95e3a10656aca76249a8fc9b16b7aa46afbed1f`。
- 候选生产 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016033321.jar`。
- 状态-only PR #483 已通过 Java/Maven、版本真值守卫及 Chromium + WebKit 双 CI，并 squash 合入 `main`。
- V4K Stable main SHA：`c28f622911e78df7308e2d601db4e434fa7ecb32`。
- 精确 Stable main CI #1147 双 CI 全绿，main 构建产物上传成功。
- Deploy Production #210 已成功完成最终状态-only 生产发布。
- 最终服务器 Maven：471 tests / 0 failures / 0 errors / 88 skipped。
- 最终 18080 预发布完整冒烟：通过。
- 最终 8081 金丝雀完整冒烟：通过；Nginx 成功切流到 8081。
- 最终 8080 正式完整冒烟：通过；Nginx 恢复到 8080，8081 金丝雀已停止。
- 最终生产 JAR SHA256：`6d41373d99cefd3f14dbd6966dd83e548c98bfbf6683a7ca2c684ee27994a0dd`。
- 当前生产 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016035217.jar`。
- 上一版 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016033321.jar`，继续保留可回滚。
- 最终仓库真值：`20261016-v4k / V4K · STABLE PRODUCTION / V4K 已上线`。
- 最终 `main` 与 `deploy/production` 请求均精确指向 `c28f622911e78df7308e2d601db4e434fa7ecb32`。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 不修改原 V4J 队列服务和原售后处理写入逻辑。
- 不修改订单状态、售后状态迁移、退款、收款、微信支付或企业微信写入。
- 不启用 V3H 小程序真实支付。
- 旧后台、原售后处理页和上一生产 release 均继续保留。
- Stable 状态收口只修改版本真值、页面说明、契约测试与开发记录，不修改 V4K 业务实现。

## 测试

- 服务层：三档互斥时效桶和全部需跟进集合。
- 服务层：SELF / DEPARTMENT / ALL / DENIED 数据范围。
- 服务层：原 status / category / followup / q 条件继续组合生效。
- 服务层：`order:view` 缺失拒绝访问，非法时效值返回参数错误。
- 控制器：保持现有 QueueSnapshot JSON 形状。
- 页面路由：独立模板，不替换原售后处理页。
- WebKit：从原概览快捷入口进入时保留 status / category / followup / q，页码归零。
- Chromium：时效队列从 URL 恢复筛选，分页更新 URL，并提供返回原售后处理页的业务编号入口。
- 统一版本：CONSOLE_VERSION / CONSOLE_STAGE / BUSINESS_VERSION、后台首页、更新记录、售后页面、运行版本漂移与 RBAC 契约。
- 生产：服务器 Maven 与 18080 → 8081 → 8080 两轮完整生产门禁均通过。

## 下一步

V4K 已完成最终稳定收口。下一阶段进入 V4L：在保持 V4K 默认行为与只读边界的前提下，为待跟进队列增加“最久未响应优先 / 最新待跟进优先”的可分享排序能力。
