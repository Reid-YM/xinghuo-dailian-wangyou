# 2026-10-17 · 发布观察人工确认与变更关联

## 阶段目标

承接 #520–#524 的运行健康、生产发布真值、异常阈值、发布后 5/15/30 分钟观察与历史稳定性基线，在不改变生产发布真值边界的前提下增加：

1. 当前 release 的人工观察结论留痕；
2. release / Git SHA / merged PR 的变更关联；
3. “确认稳定”与“需要继续观察”两类追加式人工记录；
4. 生产版本变化时拒绝旧页面误写的并发保护。

## 真值边界

生产是否发布成功仍只由生产发布网关生成的 `production-host-gateway` 主机凭证证明。人工确认：

- 不修改主机发布凭证；
- 不把人工结论写入发布历史凭证；
- 不执行部署、重启、回滚、扩容或流量切换；
- 不替代 18080 / 8081 / 8080、Nginx、current.jar SHA256 等生产门禁；
- 仅表示管理员在当前自动观察状态基础上做出的运营判断。

## 人工确认门槛

“确认稳定”只有同时满足以下条件才允许写入：

- 当前生产凭证可用；
- 当前 JVM 从发布时间起完整覆盖观察窗口；
- 30 分钟窗口为 `COMPLETE`；
- 当前发布自动严重度为 `OK`；
- 30 分钟窗口严重度为 `OK`。

如果自动观察尚未满足上述门槛，仍可由管理员选择“需要继续观察”，但必须填写原因。说明最长 240 字符。

## 并发与版本保护

浏览器提交必须携带页面最后读取到的：

- `expectedRelease`；
- `expectedGitSha`。

服务端在采样前后重新读取可信生产凭证。只要 current release 或 Git SHA 发生变化，返回冲突并拒绝写入，避免在新版本已切流后继续给旧版本页面写结论。

## 权限

接口：

- `GET /api/admin/xingwei/release-confirmation`：`deploy:view`，只读；
- `POST /api/admin/xingwei/release-confirmation`：在 `deploy:view` 基础上进一步要求 `XingweiPermissionGuard.resolve(...).allowAll()`，当前仅管理员可提交。

本阶段不新增新的 RBAC permission，也不新增数据库 migration。写请求继续受 Spring Security CSRF 保护；浏览器从既有 CSRF meta 读取 token/header 并同源提交。

## 留痕存储

人工确认使用独立目录：

`/opt/xinghuo-v2/release-confirmations`

每个 release 独立一个 `.confirmation.json` 文件，文件内容包含：

- 固定 source `admin-human-confirmation`；
- release；
- Git SHA；
- 可选 merged PR 编号；
- 追加式 events；
- 每次确认的管理员账号名、时间、结论、说明；
- 确认时自动观察严重度、覆盖状态、30 分钟窗口状态及聚合请求/5xx 数量。

不记录 IP、User-Agent、URL、查询参数、请求体、订单号或业务编号。文件只接受固定 release 名并拒绝符号链接；写入使用同目录临时文件 + atomic move，已有损坏/伪造文件失败关闭，不覆盖为“正常”。

## 发布变更关联

Deploy Production 仍先在 GitHub Actions 层验证：

- 目标等于当前 main；
- 目标来自 merged Pull Request；
- 对应 main CI 成功；
- 部署脚本语法有效。

主机在锁定精确 merge commit 后，仅从标准 `Merge pull request #...` commit subject 提取 PR 编号，并把该编号作为 schema v2 凭证的可选 `pullRequestNumber` 元数据。PR 编号只用于变更追溯，不参与主机发布真值判定；提取失败时仍以 Git SHA 为真值，后台显示 PR 关联缺失。

## 页面行为

“版本与更新记录”新增“发布观察结果留痕”工作区：

- 当前 release；
- Git SHA / PR 链接；
- 稳定确认门槛；
- 最近人工结论；
- 人工确认历史；
- 管理员确认稳定 / 继续观察操作。

读取与写入都使用 `same-origin`、`no-store`；8 秒超时。401/403 清空已展示数据并停止自动轮询；普通 `deploy:view` 账号只能查看，无法提交。

## 非本阶段范围

- 无数据库 migration；
- 不修改支付、退款、企业微信写入；
- 不新增自动回滚、自动重启、自动部署或自动扩容；
- 不自动替管理员提交人工确认；
- 不改变 Frontend Next 正式流量策略，生产前端继续保持 legacy。
