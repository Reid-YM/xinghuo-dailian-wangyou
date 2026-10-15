# V4M 售后待跟进等待时长与时效标记

日期：2026-10-16

状态：功能开发完成，保持 V4L Stable Production 真值，尚未进入统一 Release Candidate。

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
- 功能阶段继续保留 `20261016-v4l / V4L · STABLE PRODUCTION` 生产真值；attention JS 资产缓存也暂不提前切 V4M，统一 RC 时再同步。

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

## 测试范围

- 服务层验证 24 小时内记录返回 `WITHIN_24H` 和合理 `waitingMs`。
- 服务层验证 30 小时待跟进客户补充返回 `FROM_24_TO_72H`，并且 `attentionAtMs` 等于最新未确认补充时间。
- 服务层验证 80 小时记录返回 `OVER_72H`。
- 保留 ALL / DEPARTMENT / SELF / DENIED、筛选、排序和权限测试。
- 控制器验证旧兼容字段仍存在，并新增 `attentionAtMs / waitingMs / ageBucket`。
- Chromium 验证页面直接显示 `1天6小时 · 24–72小时`，同时保留 sort、URL 恢复和分页行为。
- WebKit 继续验证从售后概览进入独立待跟进队列时保留现有筛选条件。

## 后续发布流程

1. 创建 V4M 功能 PR，保持当前 V4L Stable 版本真值不变，并通过 Java/Maven、release-state、Chromium + WebKit 双 CI。
2. 功能 PR 合入后，对精确功能 main SHA 再跑主线双 CI。
3. 精确功能 main 双绿后，单独创建 V4M 统一 Release Candidate，将版本/阶段/更新记录和 attention 资产缓存同步到 V4M 候选真值。
4. RC 双 CI、精确 main CI 和 18080 → 8081 → 8080 生产门禁全部通过后，另开 Stable 状态-only PR。
5. Stable main 双 CI 和最终状态-only生产发布完成后，才能将 V4M 标记为正式稳定上线。
