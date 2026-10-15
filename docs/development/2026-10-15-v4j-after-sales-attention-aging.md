# V4J 售后待跟进时效分布

日期：2026-10-15

状态：功能开发候选，尚未切换统一 V4J Release Candidate，未发布生产。

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

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 无订单状态写入变更。
- 无售后状态写入逻辑变更。
- 无退款、真实收款、微信支付或企业微信写入变更。
- V3H 小程序支付与企业微信写入继续独立验收。
- 本功能 PR 保持 V4I 生产版本真值与 `20261015v4i` 资产缓存参数不变；只有功能合入并通过双 CI 后，才另行建立统一 V4J Release Candidate。

## 测试覆盖

- 服务层：SELF / DEPARTMENT / ALL / DENIED 数据范围。
- 服务层：仅 OPEN 或未确认客户补充进入需跟进集合。
- 服务层：24 小时内 / 24–72 小时 / 72 小时以上互斥分桶。
- 服务层：缺少 `order:view` 时拒绝访问。
- 控制器：四个稳定 JSON 字段。
- Chromium：四个时效数字渲染、统计定义说明、现有筛选和 URL 不被时效概览读取修改。

## 下一步

1. 创建 V4J 功能 PR。
2. 运行 Java/Maven、版本真值守卫与 Chromium/WebKit 双 CI。
3. CI 全绿后合入 main，并确认没有覆盖并行 Frontend Next 成果。
4. 另开统一 V4J Release Candidate，只在该阶段切 `20261015-v4j`、Release History 和资产缓存版本。
5. 继续使用精确 main SHA + `deploy/production` + 18080 → 8081 → 8080 受控生产门禁。
