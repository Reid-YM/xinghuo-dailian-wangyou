# N10 · 服务详情预览链路（仅开发）

状态：开发分支完成首版并补充慢请求/路由切换保护，未部署、未合并、未切换正式流量。

## 目标

在 N9 已提供 `/next/services` 服务大厅候选入口的基础上，补齐 Next 内部的服务详情链路，避免用户从新版服务大厅点击“详情”后立即回到旧版页面。

- 新版候选详情：`/next/service/{slug}`
- 旧版稳定详情：`/service/{slug}`（继续保留，不重定向、不删除）
- 新版详情询价：`/next/quote?service={slug}`，复用 N2 已有的服务自动预选能力

## 开发实现

- `FrontendNextPreviewController` 新增 `/next/service/{slug}` 显式预览路由。
- 新增 `n10-service-detail.js`，仅使用现有公开 `GET /api/services` 与 `GET /api/catalog` 数据。
- Next 环境中的 `/service/{slug}` 服务卡片链接通过只读运行时改写为 `/next/service/{slug}`；使用 `MutationObserver` 覆盖异步渲染卡片。
- N10 详情页显示真实公开服务名称、公开描述、游戏/分类、公开价格口径、预计时间和封面。
- 详情页“带入此服务询价”进入 `/next/quote?service=...`，由现有 N2 查询参数逻辑完成服务预选。
- 页面 canonical 仍指向旧版稳定 `/service/{slug}`，Next 预览详情设置 `noindex,follow`，避免预览页面参与重复收录。
- 页面保留一个显式旧版详情链接，并通过 `data-n10-legacy` 豁免自动改写，便于开发对照与回退检查。
- 详情请求增加递增版本号；只有当前最新请求且浏览器仍停留在同一个 slug 时才允许渲染，避免前进/后退、快速切换或慢网络让旧结果覆盖新页面。
- 请求失败显示明确错误和“重新读取”按钮；重试仍走同一只读接口和最新请求保护。

## 安全边界

- 不修改旧 `/service/{slug}` 控制器和 Thymeleaf 页面。
- 不修改 `/services`、`/quote`、`/account` 正式默认路由。
- 不启用 `app.frontend-next.default-enabled`。
- 不调用询价提交、订单、支付、退款、售后或企业微信写接口。
- 不新增数据库迁移。
- 不包含部署、合并或正式切流动作。

## 验证

新增并扩展 `FrontendNextN10ServiceDetailContractTest`，覆盖：

- Next 服务详情显式预览路由与运行时注入；
- 旧详情路由继续保留且无重定向；
- 只读公开目录接口；
- Next 内部服务详情链接闭环；
- 显式旧版链接豁免；
- 询价服务参数带入；
- canonical / robots 预览 SEO 边界；
- 过期详情响应不可覆盖当前路由；
- 错误重试仍使用同一路由同步流程；
- 禁止业务写接口和 `innerHTML` / `localStorage`。

当前仅完成代码与契约开发；未在本对话执行环境中声称 Maven / Playwright 已实际运行通过。
