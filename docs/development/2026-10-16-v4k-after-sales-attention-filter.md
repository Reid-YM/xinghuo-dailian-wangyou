# V4K 售后待跟进时效快捷队列

日期：2026-10-16

状态：统一 Release Candidate，未发布生产。

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
- 功能 PR #481 已通过 Java/Maven、版本真值守卫以及 Chromium + WebKit 全量交互回归；首次浏览器失败仅来自新增测试夹具缺少 HTML Content-Type，产品代码未改，测试夹具修正后第二轮双 CI 全绿。
- PR #481 已 squash 合入 `main`，功能主线 SHA 为 `b827aca2dab5cfcf8e23c426676a752a62de3445`。
- 精确功能主线 CI #1143 已再次通过 Java/Maven、版本真值守卫和 Chromium + WebKit 双 CI。
- 统一候选版本已切为 `20261016-v4k` / `V4K · RELEASE CANDIDATE`。
- Release History 中 V4K 保持“待发布”，V4J 继续保持“已上线”。
- 售后主页面、独立时效页面及相关 runtime 资产缓存统一为 `20261016v4k`。

## 风险边界

- 无数据库迁移。
- 无生产配置变更。
- 不修改原 V4J 队列服务和原售后处理写入逻辑。
- 不修改订单状态、售后状态迁移、退款、收款、微信支付或企业微信写入。
- 不启用 V3H 小程序真实支付。
- 旧后台、原售后处理页和上一生产 release 均继续保留。
- 当前只切统一 V4K Release Candidate；生产门禁完成前 V4K 必须保持“待发布”。

## 测试

- 服务层：三档互斥时效桶和全部需跟进集合。
- 服务层：SELF / DEPARTMENT / ALL / DENIED 数据范围。
- 服务层：原 status / category / followup / q 条件继续组合生效。
- 服务层：`order:view` 缺失拒绝访问，非法时效值返回参数错误。
- 控制器：保持现有 QueueSnapshot JSON 形状。
- 页面路由：独立模板，不替换原售后处理页。
- WebKit：从原概览快捷入口进入时保留 status / category / followup / q，页码归零。
- Chromium：时效队列从 URL 恢复筛选，分页更新 URL，并提供返回原售后处理页的业务编号入口。
- 统一版本：CONSOLE_VERSION / CONSOLE_STAGE / BUSINESS_VERSION、后台首页、更新记录、售后候选页面、运行版本漂移与 RBAC 契约。

## 下一步

1. 为统一 V4K Release Candidate 创建 PR。
2. 跑 Java/Maven、`validate-xingwei-release-state.sh` 与 Chromium/WebKit 双 CI。
3. CI 全绿后合入 `main`，再对精确主线 SHA 复核双 CI。
4. 只有 `deploy/production` 的 18080 预发布、8081 金丝雀、8080 正式服务与发布后核验全部完成后，才单独把 V4K 从“待发布”同步为“已上线”。
