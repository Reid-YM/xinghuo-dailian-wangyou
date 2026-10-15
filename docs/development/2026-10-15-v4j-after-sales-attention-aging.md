# V4J 售后待跟进时效分布

日期：2026-10-15

状态：统一 V4J Release Candidate，尚未发布生产。

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
- 统一候选版本已切为 `20261015-v4j` / `V4J · RELEASE CANDIDATE`，V4J Release History 保持“待发布”，V4I 继续保持“已上线”。
- 售后页 CSS/JS 资产缓存参数提升为 `20261015v4j`。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 无订单状态写入变更。
- 无售后状态写入逻辑变更。
- 无退款、真实收款、微信支付或企业微信写入变更。
- V3H 小程序支付与企业微信写入继续独立验收。
- 功能 PR 阶段保持 V4I 生产真值；统一 RC 阶段仅切版本、阶段、更新记录、页面说明和资产缓存，不把 V4J 提前标记为“已上线”。
- 旧后台与当前 V4I 生产 release 均继续保留，生产门禁失败时不改变现网真值。

## 测试覆盖

- 服务层：SELF / DEPARTMENT / ALL / DENIED 数据范围。
- 服务层：仅 OPEN 或未确认客户补充进入需跟进集合。
- 服务层：24 小时内 / 24–72 小时 / 72 小时以上互斥分桶。
- 服务层：缺少 `order:view` 时拒绝访问。
- 控制器：四个稳定 JSON 字段。
- Chromium：四个时效数字渲染、统计定义说明、现有筛选和 URL 不被时效概览读取修改。
- 统一版本：CONSOLE_VERSION / CONSOLE_STAGE / BUSINESS_VERSION、更新记录、后台首页、售后资产缓存、运行版本漂移与 RBAC 契约。

## 下一步

1. 为统一 V4J Release Candidate 创建 PR。
2. 运行 Java/Maven、`validate-xingwei-release-state.sh` 与 Chromium/WebKit 双 CI。
3. CI 全绿后合入 `main`，再对精确主线 SHA 复核双 CI。
4. 使用现有 `deploy/production` 受控流程执行 18080 → 8081 → 8080 生产门禁。
5. 只有候选版本真实通过生产门禁和发布后核验后，才另开状态-only PR 将 V4J 从“待发布”同步为“已上线 / STABLE PRODUCTION”。
