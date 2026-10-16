# N14 · Next 内部路由连续性（仅开发）

## 目标

N9–N13 已经补齐服务入口、服务详情、相关服务、询价上下文和业务记录预览，但审计仍发现 Frontend Next 内存在普通内部链接会回到旧前端的情况。

最明确的断点是首页游戏卡片：`shell.js` 在游戏存在 `seoSlug` 时生成 `/game/{slug}`，因此用户从 `/next` 点击游戏会离开 Next。N2 服务大厅本身已经支持 `game` 查询参数，无需新增游戏详情页或新 API。

## 本阶段实现

新增 `src/main/resources/static/next/n14-route-continuity.js`，仅在 `/next` 路径族中运行：

- 普通 `/services` 链接归一到 `/next/services`；
- 普通 `/quote` 链接归一到 `/next/quote`；
- 普通 `/account` 链接归一到 `/next/account`；
- 普通 `/service/{slug}` 链接归一到 `/next/service/{slug}`；
- 普通 `/record/{businessNo}` 链接归一到 `/next/record/{businessNo}`；
- 首页 `/game/{seoSlug}` 游戏卡片使用 N2 已加载的真实 catalog 匹配 `seoSlug -> code`，进入 `/next/services?game={code}`；
- 原查询参数与 hash 尽量保留，`game` 参数由真实 catalog code 覆盖；
- 只处理同源链接，不处理外部 URL。

## 旧版回退边界

N14 不取消旧前端，也不把旧入口做重定向：

- 带 `legacy=1` 的链接明确跳过；
- N10 的 `data-n10-legacy=1` 明确跳过；
- N13 的 `data-n13-legacy=1` 明确跳过；
- N14 预留 `data-n14-legacy=1` 作为后续显式旧版回退标记；
- `/game/{slug}`、`/service/{slug}`、`/record/{businessNo}` 等旧正式路由本身仍保持不变。

因此 N14 的作用只是让 **Next 页面里的普通主路径继续留在 Next**，不是替换旧站路由。

## 异步内容

服务/游戏卡片由真实 API 数据异步渲染。N14 使用：

- 初次 DOM 扫描；
- `MutationObserver` 监听新增链接和 href 变化；
- 捕获阶段 click 再做一次即时归一；
- `xh:pagechange` 后重新扫描。

这样不会依赖固定渲染时序，也不需要修改 N2 的数据请求流程。

## 数据与安全边界

N14：

- 不调用 `fetch`；
- 不新增 POST/PATCH/PUT/DELETE；
- 不读写 localStorage / sessionStorage；
- 不新增业务 API；
- 不修改询价提交、订单、支付、退款或账号安全协议；
- 不使用 `innerHTML` / `insertAdjacentHTML`；
- 游戏筛选只使用 N2 已加载的公开 catalog 数据。

## 接入

`FrontendNextPreviewController` 增加：

- `N14_RUNTIME`；
- `/next/n14-route-continuity.js` 运行时注入；
- N14 文件内容指纹缓存版本。

新增静态契约测试 `FrontendNextN14RouteContinuityContractTest`，覆盖运行时接入、普通路由归一、明确 legacy 例外、真实游戏 code 映射、异步 DOM 和只读边界。

## 当前状态

N14 当前仅提交到 Frontend Next 开发分支：

- 不部署；
- 不合并到 `main`；
- 不修改正式前端默认入口；
- 不切换生产流量。

本阶段未执行 Maven / Playwright，因此不宣称自动化测试已通过；只新增了对应静态契约与代码级边界。
