# 发布复核时延预算与优先级解释（2026-10-17）

## 背景

PR #526 已把最近可信生产 release、人工确认状态与 GitHub PR 文件类别汇总成发布复核队列；PR #527 修复风险服务的 Spring 构造器注入并完成生产发布。

复核队列自身仍有一个稳定性风险：每次刷新最多会对最近 4 个 PR 顺序读取 GitHub 文件列表。旧实现单页请求超时 1800ms、每个 PR 最多 3 页，并且 UNKNOWN 不缓存；GitHub 抖动时，外部风险查询可能逼近后台页面 8 秒客户端超时，同时 60 秒自动刷新会重复请求失败的 PR。

## 本阶段目标

让“运营稳定性工具”自身具备明确的外部查询预算、短期失败缓存和服务端可解释的人工复核优先级，同时保持所有生产真值与人工确认边界不变。

## 风险查询预算

`XingweiReleaseChangeRiskService` 使用以下固定规则：

- 单 PR 总外部查询预算：1200ms。
- 单次 GitHub HTTP 请求超时上限：700ms。
- 单 PR 最多读取 3 页，每页最多 100 个 changed files。
- 剩余预算不足 100ms 时不再发起下一页请求，返回 `UNKNOWN / LOOKUP_BUDGET_EXHAUSTED`。
- GitHub 不可用或响应格式不可信时返回 UNKNOWN；绝不降级成 LOW。
- 文件列表超过可验证页数时返回 `UNKNOWN / PR_FILE_LIST_TRUNCATED`。

这只是外部风险查询预算，不是整个 `/release-review-queue` 接口 SLA。接口还包含本机可信凭证、确认记录和历史文件读取。

## 缓存规则

- 完整可信风险结果：进程内缓存 6 小时。
- UNKNOWN / 外部失败 / 列表不完整：进程内短缓存 90 秒。
- JVM 重启后缓存自然清空。
- 短缓存的目的仅是避免 60 秒自动刷新持续放大外部故障；不会把 UNKNOWN 伪装为已知风险。

## 人工复核优先级

优先级由服务端根据当前确认状态和透明风险标签生成，并返回 `reviewPriority + priorityReason`：

- `REVIEW_NOW`：当前 release 已满足稳定确认门槛，但风险为 HIGH 或 UNKNOWN，需要优先人工核对。
- `REVIEW_READY`：当前 release 已满足稳定确认门槛，且风险信息完整为 MEDIUM/LOW，可按正常人工流程确认。
- `FOLLOW_UP`：管理员已标记继续观察。
- `OBSERVE`：30 分钟自动观察尚未完成或尚未满足稳定确认门槛。
- `BLOCKED`：确认或生产发布真值不可用，先恢复可信数据。
- `COMPLETE`：当前 release 已有人工作出稳定确认。
- `ARCHIVED_COMPLETE`：历史 release 已有稳定确认，仅保留审计记录。
- `ARCHIVED_ATTENTION`：历史 release 曾标记继续观察，已退役不可追认。
- `ARCHIVED_GAP`：历史 release 缺少可信稳定结论或确认记录异常，已退役不可补写。

UNKNOWN 会提高人工复核优先级，不会降低优先级。

## 后台可视化

新增只读辅助脚本 `admin-xingwei-release-review-priority.js`。它不发起新的 HTTP 请求，只消费现有复核队列写入的 `window.xwrReleaseReviewQueue` 快照，展示：

- 当前 release 的人工复核优先级；
- 服务端本次风险解析耗时；
- 本次外部风险查询预算；
- UNKNOWN 风险数量；
- 服务端生成的优先级原因；
- 单 PR 预算、成功缓存 TTL 和 UNKNOWN 短缓存 TTL。

该辅助脚本在 401/403 或队列清空后同步失败关闭，不保留独立敏感快照。

## 边界

- 风险标签和优先级只用于人工复核排序与解释。
- 不自动确认稳定。
- 不自动回滚、重启、部署或扩容。
- 不修改生产主机发布凭证来源和校验规则。
- 不允许历史 release 追认稳定。
- 不新增数据库 migration。
- 不修改支付、退款、企业微信写入。
- 不切换 Frontend Next 正式流量；Legacy 继续正式前端。

## 验证重点

- HIGH/MEDIUM/LOW 分类规则保持不变。
- 预算耗尽、GitHub 不可用和列表不完整均保持 UNKNOWN。
- 当前 READY + UNKNOWN/HIGH => `REVIEW_NOW`；READY + MEDIUM/LOW => `REVIEW_READY`。
- 历史未确认继续为 `ARCHIVED_GAP`，不可操作。
- 辅助 UI 不调用 `fetch()`，避免为了展示预算再次制造外部风险查询。
- Chromium/WebKit 回归验证优先级、预算、缓存规则文本与原有 CSRF 人工确认链路共存。
