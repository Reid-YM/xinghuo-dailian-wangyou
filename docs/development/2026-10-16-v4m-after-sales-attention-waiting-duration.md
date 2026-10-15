# V4M 售后待跟进等待时长与时效标记

日期：2026-10-16

状态：统一 Release Candidate，尚未发布生产。

## 目标

V4L 已上线待跟进队列优先级排序，可按最新待跟进或最久未响应查看记录。V4M 继续做只读运营增强：直接展示每条当前待跟进记录已经等待多久，以及它属于 24 小时内、24–72 小时或 72 小时以上哪一档，让客服无需根据时间戳自行判断。

## 本阶段实现

- 保持 `GET /api/admin/xingwei/business/after-sales/cases/attention-queue` 路径、筛选参数、分页和权限边界不变。
- 为 attention queue 使用专用只读响应 DTO，避免扩散修改普通售后队列共享模型。
- 顶层响应继续保持 `items / totalCount / page / pageSize / status / category / followup / query / hasNext` 兼容形状。
- 每条 attention item 追加：
  - `attentionAtMs`：当前真正需要客服响应的起算时间；
  - `waitingMs`：服务端按当前时间计算的已等待毫秒数，最小为 0；
  - `ageBucket`：`WITHIN_24H / FROM_24_TO_72H / OVER_72H`。
- `attentionAtMs` 继续复用 V4J/V4K/V4L 统一语义：
  - 有尚未确认的新客户补充时，以最新客户补充时间为准；
  - 否则仅 OPEN 记录以原始售后提交时间为准。
- `ageBucket` 与既有时效筛选边界完全一致：
  - 等待不足 24 小时：`WITHIN_24H`；
  - 等待至少 24 小时且不足 72 小时：`FROM_24_TO_72H`；
  - 等待至少 72 小时：`OVER_72H`。
- 独立待跟进页面新增“等待时长”列，显示例如 `1天6小时 · 24–72小时`。
- 不使用“SLA”措辞；24h/72h 只是现有运营时效分档，不代表新的服务承诺。

## 功能合并与精确主线验证

- 功能 PR #488：`V4M: 售后待跟进等待时长与时效标记`。
- PR #488 的 Java/Maven、版本真值守卫、Chromium + WebKit 实际交互回归全部通过。
- PR #488 已 squash 合入 `main`。
- 功能主线 SHA：`6736ee80d6d2a927f2f63e37cd869d82b6acb76f`。
- 精确功能主线 CI #1157 已再次通过：
  - Java 21 / Maven Build：通过；
  - Xingwei release-state：通过；
  - main build artifact：上传成功；
  - Chromium + WebKit UI Regression：通过。

## 统一 Release Candidate

- RC 分支：`feat/v4m-unified-release`，精确基于功能主线 `6736ee80d6d2a927f2f63e37cd869d82b6acb76f`。
- `CONSOLE_VERSION`：`20261016-v4m`。
- `CONSOLE_STAGE`：`V4M · RELEASE CANDIDATE`。
- `BUSINESS_VERSION`：`20261016-v4m`。
- Release History 新增 V4M，状态保持 `待发布`。
- V4L 继续保持 `已上线`，并保留最终 Stable PR #486、Deploy Production #212 生产证据。
- 后台首页、业务工作台、更新记录、售后处理页、独立待跟进队列与版本/运行时/RBAC/总联调契约同步到 V4M 候选真值。
- attention JS cache 更新为 `20261016v4m`；其他未变化的 V4K 售后资产不无意义重刷缓存。
- V4M 候选尚未经过生产门禁，因此不能标记 `已上线`。

## 兼容性与风险边界

- 无数据库迁移。
- 无生产配置变更。
- 无订单写入。
- 无售后状态或售后处理写入变更。
- 无自动催单、自动升级或自动改变优先级。
- 不修改退款、真实收款、微信支付或企业微信写入。
- `order:view` 与 ALL / DEPARTMENT / SELF / DENIED 数据范围继续执行。
- V3H 小程序支付继续待真机验收，PR #372 企业微信写入继续独立验收。
- 默认 `NEWEST_FIRST` 与 V4L 的 `OLDEST_FIRST` 排序行为均保持不变。
- 旧后台与 V4L 生产 release 继续保留可回滚。

## 测试范围

- 服务层验证 24 小时内记录返回 `WITHIN_24H` 和合理 `waitingMs`。
- 服务层验证 30 小时待跟进客户补充返回 `FROM_24_TO_72H`，并且 `attentionAtMs` 等于最新未确认补充时间。
- 服务层验证 80 小时记录返回 `OVER_72H`。
- 保留 ALL / DEPARTMENT / SELF / DENIED、筛选、排序和权限测试。
- 控制器验证旧兼容字段仍存在，并新增 `attentionAtMs / waitingMs / ageBucket`。
- Chromium 验证页面直接显示 `1天6小时 · 24–72小时`，同时保留 sort、URL 恢复和分页行为。
- WebKit 继续验证从售后概览进入独立待跟进队列时保留现有筛选条件。
- 版本契约：V4M 当前必须为 Release Candidate / 待发布，V4L 必须继续为已上线。
- 运行时契约：页面声明与 `/api/admin/xingwei/health` 返回 V4M 候选真值时匹配；旧运行版本仍触发版本漂移。

## 下一步

1. 为统一候选创建独立 PR，并通过 Java/Maven、版本真值守卫、Chromium + WebKit 双 CI。
2. 合入后对精确 V4M RC `main` SHA 再跑主线双 CI。
3. 只有精确 RC main 双 CI 全绿后，才通过现有 `deploy/production` 受控链路执行 18080 → 8081 → 8080 生产门禁。
4. 生产门禁成功后，另开状态-only PR 将 V4M 同步为 `V4M · STABLE PRODUCTION / 已上线`。
5. Stable PR 合入并通过精确 main 双 CI 后，再执行一次状态-only 生产发布，使仓库真值与生产运行真值完全一致。
