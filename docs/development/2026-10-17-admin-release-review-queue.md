# #526 发布复核队列与透明变更风险

## 目标

承接 #524 的发布历史和 #525 的人工确认留痕，把单个当前 release 的人工确认扩展为最近多版本的只读运营汇总，同时保持生产发布真值、自动观察和人工结论三者边界清晰。

## 发布复核队列

新增 `GET /api/admin/xingwei/release-review-queue`：

- `deploy:view`；
- `no-store`；
- 只读；
- 最近最多 10 个可信生产 release；
- 汇总人工状态：`STABLE / NEEDS_ATTENTION / PENDING / UNCONFIRMED / INVALID`；
- 当前 release 根据 #525 的真实 30 分钟门槛显示 `WAITING / READY / FOLLOW_UP / COMPLETE / BLOCKED`；
- 已退役 release 永远不允许追认稳定，只能显示 `ARCHIVED_UNCONFIRMED / ARCHIVED_ATTENTION / COMPLETE / ARCHIVED_INVALID`。

历史确认文件继续使用 #525 的独立目录与格式，不修改生产主机发布凭证。

## 透明变更风险

风险只用于人工复核优先级，不参与发布成功、自动回滚或人工稳定结论。

数据来源：生产凭证中已有的 PR 编号 + GitHub 公共 PR files API。应用最多为最近 4 个 release 查询，完整结果按 PR 号缓存。接口超时、限流、PR 缺失或文件列表超过读取上限时返回 `UNKNOWN`，不得降级为 `LOW`。

规则版本：`v1-20261017`。

### HIGH

- `DATABASE`：Flyway migration；
- `PAYMENT`：支付模块；
- `SECURITY`：安全模块；
- `DEPLOYMENT`：生产部署 workflow / deployment scripts。

### MEDIUM

- `WECOM`
- `ACCOUNT`
- `OBSERVABILITY`
- `ADMIN_UI`
- `BACKEND`
- `LEGACY_FRONTEND`
- `TOOLING`
- `OTHER`

### LOW

仅包含 `TESTS / DOCS / FRONTEND_NEXT` 时为 LOW。

风险 API 对页面只返回：风险级别、变更文件数量、类别、规则版本和来源状态；不返回具体文件路径。

## UI

继续使用“版本与更新记录”中的人工确认区域，不新增独立导航入口。现有确认脚本动态增加“最近发布人工复核队列”：

- 追踪版本数；
- 已稳定；
- 继续观察；
- 未确认；
- 每个 release 的 PR / Git SHA；
- 当前/历史确认状态；
- 透明风险级别、文件数量和类别；
- 当前风险规则版本与边界说明。

401/403 时队列数据清空；网络失败仅标记本次复核失败，不产生人工确认写入。

## 不变边界

- 无数据库 migration；
- 不修改支付、退款、企业微信业务写入；
- 不自动部署、回滚、重启或扩容；
- 不自动替管理员确认稳定；
- 不允许历史 release 追认稳定；
- Legacy 继续是正式前端；Frontend Next 仍只有独立 `/next` 入口。
