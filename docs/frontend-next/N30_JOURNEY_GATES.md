# N30 · 正式切换前真实路径验收（第一阶段）

## 本轮核对的真实遗漏

N15 账号登录回跳最初只覆盖 N1–N13 的 `/services`、`/quote`、`/account`、服务与业务记录详情。N23–N28 新增了更多 `/next/**` 显式预览页，但如果登录 `next` 参数仍使用早期 Legacy 路径，N15 不会统一映射这些页面。结果是登录后可能离开 Next 预览体系。

本轮只补齐确实已有 Next 路由的以下安全映射：攻略中心及攻略详情、MW4 工具站、投稿工作台、业务记录中心、客服、信任、规则及隐私。保留站内查询参数与锚点，不改变 `legacy=1` 显式回旧版、认证循环阻断、外站 URL 拦截及 `/admin`、`/api`、`/logout` 安全约束。

## 新增浏览器验收

- `FrontendNextN30AuthReturnJourneyPlaywrightTest`：Chromium 和 WebKit 验证新版所有后续页面的登录回跳；旧版显式 escape、原 `/guest-access` 独立链路、外站与管理/API 入口的边界保持不变。
- `FrontendNextN30QuoteRecordJourneyPlaywrightTest`：Chromium 和 WebKit 在网络模拟中从 N3 真实提交运行时（请求 `POST /api/quote` 带幂等键）获取业务编号、由 N26 history bridge 写入与旧版相同的本机 `xh_client_records_v1`，再走 N13 `GET /api/records/{businessNo}` 详情以及 N26 `/next/records` 列表，验证持续可返回单条真实记录并且没有重复提交。
- 这些验收使用本地 Playwright 路由模拟服务器返回（非生产业务请求）。它们验证浏览器内真实 JS 模块协作，但不声称已经完成线上全链路、真实微信支付、真实订单履约或账号授权验收。

## 剩余 N30 必要条件

1. 在集成分支完整跑 Java 21/Maven 及 Chromium/WebKit CI；验证与最新 main 并存。
   - 本次集成验证基线为 `main@e1a15c664f25bef3b39154f2258f2d11314b90e1`；不修改此基线上的订单、支付与小程序提交。
2. 游客询价、实际 Session 登录/回跳、订单跟踪以及线下支付/退款/售后业务需按其现有真实权限与状态机另做隔离验收；不得为了 Next 测试创建伪交易。
3. 每项潜在切换风险需明确 rollback、监控、灰度开关和生产回归边界，正式切流单独获得用户授权。
4. N23–N30 未合并/部署前，新增 `/next/**` 不能宣称生产可用。

## 发布与安全边界

本阶段只改 N15 Next 客户端登录回跳，另增测试/文档；不改支付、账号 API、订单服务、权限、数据库或正式 Legacy 首页。默认继续 `app.frontend-next.default-enabled=false`。
