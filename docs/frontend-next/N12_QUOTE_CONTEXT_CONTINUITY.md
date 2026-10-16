# N12 · 询价上下文连续性（仅开发）

状态：开发分支完成首版，未部署、未合并、未切换正式流量。

## 目标

N10 已允许从 `/next/service/{slug}` 进入 `/next/quote?service={slug}`，N11 又补齐了详情页的相关服务连续浏览。本阶段解决进入询价页后的上下文断层：用户需要明确看到当前正在询哪个服务，手动更换服务后 URL 也应与选择保持一致，并能随时返回对应的 Next 服务详情。

## 开发实现

- 新增 `n12-quote-context.js`，由 `FrontendNextPreviewController` 注入并纳入内容指纹版本。
- 不新增数据接口，直接复用 `XHFrontendNextN2.state.services` 与 `state.catalog` 已加载的真实公开目录。
- `/next/quote?service={slug}` 会把 URL 中的服务恢复到 `qGame / qCat / qSvc` 三个现有选择器。
- 从服务大厅点“询价”后，即使原 N2 先完成本地预选再通过 `go('quote')` 进入询价页，N12 也会把当前选择同步成 `?service={slug}`。
- 手动修改游戏、分类或服务时，当前选择重新渲染，并通过 `history.replaceState` 更新 `service` 参数；不会使用 `pushState` 制造额外历史记录。
- URL 更新基于 `new URL(location.href)`，只设置或删除 `service`，保留 `utm_source`、`utm_medium`、`utm_campaign` 等其它已有查询参数。
- 询价表单上方增加“当前询价服务”上下文卡：展示真实公开服务名称、游戏、分类、参考价格、预计时间和公开说明。
- 有有效 slug 时提供“返回服务详情”，目标保持 `/next/service/{slug}`；同时提供“重新选择”快速聚焦现有服务选择框。
- URL 指向已下架或不存在的服务时，不伪造服务信息，明确提示当前不可用并保留浏览服务大厅入口。
- 监听 `xh:pagechange`、`popstate` 和服务选择器 option 变化，覆盖 SPA 内切换、浏览器前进后退以及 N2 异步目录加载后的恢复场景。

## 安全与兼容边界

- 不修改 N3 的询价提交 payload、`POST /api/quote`、截图上传或 `Idempotency-Key` 逻辑。
- 不新增任何 fetch/API 请求。
- 不修改旧 `/quote` 正式页面或旧 `/service/{slug}`。
- 不使用 `innerHTML`、`insertAdjacentHTML`、`localStorage` 或 `sessionStorage`。
- 不涉及订单、支付、退款、售后、企业微信写入或数据库迁移。
- 不启用 `app.frontend-next.default-enabled`，不包含部署、合并或正式切流动作。

## 验证

新增 `FrontendNextN12QuoteContextContractTest`，覆盖：

- N12 运行时注入与内容指纹；
- 只复用 N2 已加载的公开目录，不新增 API；
- URL → 选择器与选择器 → URL 双向同步；
- `replaceState` 更新且保留其它查询参数；
- 前进后退、页面切换和异步 option 更新监听；
- Next 详情返回链接和下架服务提示；
- N3 原有询价提交、截图上传与幂等键逻辑仍作为独立职责保留；
- 禁止不安全 DOM 拼接和本地持久化。

当前仅完成代码与契约开发；未在本对话执行环境中声称 Maven / Playwright 已实际运行通过。
