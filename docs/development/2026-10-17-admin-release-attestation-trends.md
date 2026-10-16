# 2026-10-17 · 后台发布真值凭证与近期错误趋势

## 目标

承接 #520 / #521 的运行健康基线，把“应用能够自行证明的运行状态”和“生产主机发布流程能够证明的发布状态”明确拆开，并在新版后台“版本与交付”页集中只读展示。

## 本阶段交付

- 现有进程内请求指标增加最近 15 分钟分钟级趋势：总请求、4xx、5xx。
- 趋势仍不记录 URL、查询参数、请求体、账号、IP、订单号、业务编号或异常文本；JVM 重启后自然清零。
- 新增 `/api/admin/xingwei/release-attestation`，继续使用 `deploy:view`，响应 `no-store`。
- `deploy-from-github.sh` 只有在 18080 预发布、8081 金丝雀、8080 正式入口、Nginx 回切、端口清理和 `current.jar` SHA256 一致性全部通过后，才原子写入 `/opt/xinghuo-v2/release-attestation.json`。
- 发布凭证只包含发布时间、Git SHA、JAR SHA256、当前/上一 release 文件名、正式/金丝雀/Nginx 端口及三层冒烟结果；不包含服务器绝对路径、密钥、环境变量或业务数据。
- 应用只读该凭证。凭证缺失、损坏、端口矛盾、金丝雀未停止或任何冒烟项不完整时，一律返回不可用，不自行推断“发布成功”。
- “版本与更新记录”页新增生产主机发布真值区，并在运行健康区增加 15 分钟请求错误趋势。

## 真值边界

1. JVM 健康、数据库 `SELECT 1`、请求/错误趋势属于应用运行真值。
2. Git SHA、JAR SHA256、正式 release、上一 release、Nginx 正式端口和三层冒烟属于生产主机发布真值，只接受发布网关成功末端写入的凭证。
3. 页面不执行部署、不执行回滚、不写入业务数据。
4. 本阶段不修改支付、退款、企业微信写入逻辑，不新增数据库 migration，不切换 Frontend Next 正式流量。

## 验证

- `XingweiRuntimeRequestMetricsTest`：锁定 15 分钟分钟桶及 4xx/5xx 聚合。
- `XingweiReleaseAttestationServiceTest`：锁定完整成功凭证、矛盾凭证和缺失凭证处理。
- `XingweiReleaseAttestationContractTest`：锁定 `deploy:view`、`no-store`、主机成功末端原子写入及三层冒烟字段。
- `XingweiReleaseObservabilityContractTest`：锁定页面真值分区、资源引用、隐私文案和响应式样式。
- `XingweiReleaseObservabilityPlaywrightTest`：覆盖趋势 5xx 标记、成功发布凭证和 403 失败关闭。
- 完整 Maven 与 Chromium/WebKit 全量回归交由 PR CI。
