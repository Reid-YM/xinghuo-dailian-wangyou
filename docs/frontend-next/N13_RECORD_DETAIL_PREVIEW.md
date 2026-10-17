# N13 · 业务记录预览链路（仅开发）

状态：开发分支完成首版，未部署、未合并、未切换正式流量。

## 目标

N12 已把服务详情与询价上下文保持在 Frontend Next 内，但 N3 提交成功后原先生成的“查看业务记录”仍指向旧 `/record/{businessNo}`。本阶段补齐提交成功后的只读记录预览，让“服务详情 → 询价 → 提交成功 → 业务记录”形成完整 Next 预览链路，同时继续保留旧记录页作为稳定对照与回退入口。

## 开发实现

- `FrontendNextPreviewController` 新增 `/next/record/{businessNo}` 显式预览路由。
- 新增 `n13-record-detail.js` 并纳入内容指纹版本。
- N3 提交成功后的“查看业务记录”链接改为 `/next/record/{businessNo}`；询价提交 payload、截图上传和幂等键逻辑均未改变。
- N13 仅调用现有公开 `GET /api/records/{businessNo}`，没有新增后端数据接口。
- 业务编号格式与 `RecordService` 保持一致：`XH` + 10 位大写字母/数字，或历史 `XH-` + 24 位排除易混字符的旧订单号。
- 页面根据公开响应展示业务编号、询价/订单阶段、状态、标题、预计时间、创建/更新时间、公开进度、百分比和客户可见备注。
- `hasProgressImage=true` 时只展示现有 `/api/records/{businessNo}/image` 只读截图。
- 请求增加递增版本与当前 URL 校验，旧慢请求不会覆盖新的记录路由。
- 找不到记录、格式无效、查询维护或接口失败时只显示明确错误/重试状态，不构造模拟业务数据。
- 页面保留显式“查看旧版记录页”链接 `/record/{businessNo}`。
- 记录预览设置 `noindex,nofollow`，canonical 指向旧稳定 `/record/{businessNo}`。

## 安全与兼容边界

- 不修改旧 `/record/{businessNo}` Controller 和 `record-detail` 模板。
- 不修改 `RecordService` 或公开记录数据结构。
- 不新增 POST / PATCH / PUT / DELETE。
- 不提供订单修改、支付、退款、售后或企业微信写操作。
- 不使用 `innerHTML`、`insertAdjacentHTML`、`localStorage` 或 `sessionStorage`。
- 不启用 `app.frontend-next.default-enabled`。
- 不包含部署、合并或正式切流动作。

## 验证

新增 `FrontendNextN13RecordDetailContractTest`，覆盖：

- Next 记录路由、运行时注入与内容指纹；
- 旧记录页继续存在且没有强制重定向；
- 只读 `GET /api/records/{businessNo}` 与公开截图；
- 业务编号规则与后端一致；
- 慢请求/过期路由响应保护；
- N3 提交成功进入 Next 记录页；
- 显式旧版记录逃生入口；
- record 页面 `noindex,nofollow` 和 canonical；
- 禁止业务写接口、不安全 DOM 拼接和本地持久化。

当前仅完成代码与契约开发；未在本对话执行环境中声称 Maven / Playwright 已实际运行通过。
