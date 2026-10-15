# V4M 售后待跟进等待时长与时效标记

日期：2026-10-16

状态：已通过候选生产门禁，Stable Production 状态收口中。

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
- 精确功能主线 CI #1157 已通过 Java/Maven、Xingwei release-state、main build artifact 与 Chromium + WebKit。

## 统一 Release Candidate

- RC 分支：`feat/v4m-unified-release`，精确基于功能主线 `6736ee80d6d2a927f2f63e37cd869d82b6acb76f`。
- RC PR #489：`V4M: 统一售后待跟进等待时长 Release Candidate`。
- PR #489 的 Java/Maven、版本真值守卫、Chromium + WebKit 双 CI 全部通过。
- RC main SHA：`dd283cccb5e7c3e64d3469e3b045447a101f216d`。
- 精确 RC main CI #1159 已通过 Java/Maven、release-state、main build artifact 与 Chromium + WebKit。
- 候选真值：`20261016-v4m / V4M · RELEASE CANDIDATE / V4M 待发布`。
- V4L 继续保持 `已上线`，并保留最终 Stable PR #486、Deploy Production #212 生产证据。
- attention JS cache 已更新为 `20261016v4m`；其他未变化的 V4K 售后资产不无意义重刷缓存。

## 候选生产门禁

- Deploy Production #213：成功。
- 部署目标：`dd283cccb5e7c3e64d3469e3b045447a101f216d`。
- 生产前置门禁：merged PR origin、精确 main CI、部署脚本、SSH 全部通过。
- Flyway 安全阀：无新增 migration。
- 服务器 Maven：472 tests / 0 failures / 0 errors / 88 skipped。
- 18080 预发布完整冒烟：通过。
- 8081 金丝雀完整冒烟：通过。
- Nginx 金丝雀切流：通过。
- 8080 正式完整冒烟：通过。
- Nginx 最终恢复到 `127.0.0.1:8080`。
- 8081 金丝雀已停止。
- 发布后核验：通过。
- 候选生产 JAR SHA256：`10f62885752346a929efe7ec8c248ad03fcafb31a77335ddda2575d0ae6411ef`。
- 当前候选生产 release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016070306.jar`。
- 上一版可回滚 V4L release：`/opt/xinghuo-v2/releases/xinghuo-v2-standard-20261016051125.jar`。

## Stable 状态收口

- Stable 分支：`release/v4m-stable-production`，精确基于已通过候选生产门禁的 RC main SHA。
- `CONSOLE_VERSION` 与 `BUSINESS_VERSION` 继续保持 `20261016-v4m`。
- `CONSOLE_STAGE` 切换为 `V4M · STABLE PRODUCTION`。
- V4M Release History 切换为 `已上线`。
- attention JS cache 继续保持 `20261016v4m`，不重复刷新没有变化的资产。
- Stable 分支只同步控制器版本真值、后台页面、运行时/RBAC/更新记录契约和本开发记录；不再次修改 V4M 等待时长业务实现。

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
- Stable 版本契约：V4M 必须为 `STABLE PRODUCTION / 已上线`，V3H 必须继续待真机验收。
- 运行时契约：页面声明与 `/api/admin/xingwei/health` 返回 V4M Stable 真值时匹配；旧运行版本仍触发版本漂移。

## 下一步

1. 创建 V4M Stable 状态-only PR，并通过 Java/Maven、版本真值守卫、Chromium + WebKit 双 CI。
2. Stable PR 合入后，对精确 Stable main SHA 再跑主线双 CI。
3. 精确 Stable main 双绿后，通过现有 `deploy/production` 受控链路执行最后一次状态-only 18080 → 8081 → 8080 生产发布。
4. 最终发布成功后，补充 Stable PR、精确 main CI、最终 Deploy Production、最终 JAR SHA256、current release 与 rollback release 到本记录。
5. 最终记录以独立 docs-only PR 合入，不再改变业务实现。
