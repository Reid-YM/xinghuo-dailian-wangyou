# V4K 售后待跟进时效快捷队列

日期：2026-10-16

状态：功能开发候选，未切换 V4J 生产版本真值。

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

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 不修改原 V4J 队列服务和原售后处理写入逻辑。
- 不修改订单状态、售后状态迁移、退款、收款、微信支付或企业微信写入。
- 不启用 V3H 小程序真实支付。
- 旧后台、原售后处理页和上一生产 release 均继续保留。
- 功能 PR 阶段保持 `20261015-v4j` / `V4J · STABLE PRODUCTION` 真值；只有统一 V4K Release Candidate 才切换版本号和更新记录。

## 测试

- 服务层：三档互斥时效桶和全部需跟进集合。
- 服务层：SELF / DEPARTMENT / ALL / DENIED 数据范围。
- 服务层：原 status / category / followup / q 条件继续组合生效。
- 服务层：`order:view` 缺失拒绝访问，非法时效值返回参数错误。
- 控制器：保持现有 QueueSnapshot JSON 形状。
- 页面路由：独立模板，不替换原售后处理页。
- WebKit：从原概览快捷入口进入时保留 status / category / followup / q，页码归零。
- Chromium：时效队列从 URL 恢复筛选，分页更新 URL，并提供返回原售后处理页的业务编号入口。

## 下一步

1. 创建 V4K 功能 PR，跑 Java/Maven、版本真值守卫、Chromium + WebKit 全量 CI。
2. 功能 CI 全绿后合入 main，并确认并行 Frontend Next 提交未被覆盖。
3. 单独建立 V4K 统一 Release Candidate，届时再切 `20261016-v4k`、Release History 和资产缓存真值。
4. 统一候选与精确 main 双 CI 通过后，再走既有 `deploy/production` 的 18080 → 8081 → 8080 生产门禁。
