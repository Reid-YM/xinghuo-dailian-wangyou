# N11 · 相关服务连续浏览（仅开发）

状态：开发分支完成首版，未部署、未合并、未切换正式流量。

## 目标

在 N10 新版服务详情预览链路完成后，继续补齐详情页内的比较与继续浏览体验：用户查看某个服务后，可以直接看到同游戏的其他公开服务，并继续停留在 Frontend Next 预览路径中。

## 开发实现

- 新增 `n11-related-services.js`，在 N10 详情成功出现后挂载“同游戏的其他服务”。
- 优先复用 `XHFrontendNextN2.state.services` 已读取的公开服务列表；缓存尚未可用时只读 `GET /api/services`。
- 根据当前服务 `gameCode` 过滤同游戏服务，排除当前 slug，最多展示 3 项。
- 每个相关服务继续链接到 `/next/service/{slug}`，不会跳回旧 `/service/{slug}`。
- 卡片沿用公开服务字段：名称、公开短描述、封面、价格口径和预计时间。
- N11 失败时只移除推荐模块，不修改 N10 主详情，不显示虚构兜底商品。
- 推荐读取同样带请求版本和当前路由校验，旧请求不能覆盖新的详情页面。
- 使用 `MutationObserver + queueMicrotask` 感知 N10 异步渲染，避免定时轮询。
- N11 资源纳入 Frontend Next 内容指纹版本，确保后续开发预览不会长期命中旧 JS。

## 安全边界

- 不新增后端业务接口或数据库迁移。
- 不调用询价提交、订单、支付、退款、售后或企业微信写接口。
- 不修改旧 `/service/{slug}` 页面。
- 不改变正式 `/services`、`/service/{slug}`、`/quote`、`/account` 默认路由。
- 不启用 `app.frontend-next.default-enabled`。
- 不部署、不合并 main、不切正式流量。

## 验证

新增 `FrontendNextN11RelatedServicesContractTest`，覆盖：

- N11 在 N10 之后加载，并参与内容指纹；
- 只读已有公开服务数据；
- 仅推荐同游戏且排除当前服务；
- 最多 3 项并保持 `/next/service/{slug}` 预览链路；
- 推荐失败不破坏主详情；
- 过期响应不可覆盖当前路由；
- 禁止业务写 API、`innerHTML` 和 `localStorage`。

当前仍只完成代码与静态契约开发；未在本对话执行环境中声称 Maven / Playwright 已实际运行通过。
