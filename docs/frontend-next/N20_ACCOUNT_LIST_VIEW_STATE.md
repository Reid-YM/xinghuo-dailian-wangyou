# N20 · 账号列表视图状态连续性

## 目标

N18 / N19 已经让询价和订单详情具备可复制深链、浏览器前进后退、上一条 / 下一条与公开业务记录回跳。N20 继续收口账号列表本身：让用户在已加载业务记录中搜索、按状态筛选、按更新时间排序，并把这些视图条件放进 URL，使列表 → 详情 → 公开业务记录 → 返回详情的过程中不丢上下文。

本阶段仍然只开发 Frontend Next，不部署、不合并 main、不切正式流量。

## 后端能力边界

当前 `ConsoleApiController` 的公开账号列表接口仍是：

- `GET /api/console/quotes` → 服务端固定读取第 0 页、最多 50 条；
- `GET /api/console/orders` → 服务端固定读取第 0 页、最多 50 条。

这两个接口当前没有向前端暴露 `page / size / status / sort` 参数。因此 N20 **不新增请求、不修改后端、不伪装服务端分页**。

N20 的搜索、状态筛选和排序只作用于 N5 已经真实读取到浏览器中的记录。界面会显示当前接口已加载数量；达到 50 条时明确提示“当前接口单次最多返回 50 条”。真正的服务端分页应当在后续后端能力明确开放后单独实现。

## URL 视图状态

仅在以下账号业务列表族生效：

- `/next/account/requests`
- `/next/account/requests/{businessNo}`
- `/next/account/orders`
- `/next/account/orders/{businessNo}`

支持三个查询参数：

- `q`：搜索关键词，最长 60 个字符；
- `status`：业务状态，只接受安全状态标识格式，且在当前列表真实加载后必须存在于实际状态集合；
- `sort`：仅支持 `updated-desc` / `updated-asc`，缺省表示服务端原始顺序。

无效状态、未知排序值和空参数会被归一化删除。参数更新使用 `history.replaceState`，避免用户每输入一个字符都制造浏览器历史记录；进入详情仍由 N18 / N19 使用正常历史导航。

## 列表行为

新增 `static/next/n20-account-list-view-state.js`：

- 搜索业务编号、游戏、需求 / 服务、状态文案和进度文案；
- 根据真实 `status` 值生成状态下拉，不写死业务状态集合；
- 支持默认顺序、最近更新优先、较早更新优先；
- 仅隐藏 / 重排 N5 已渲染的真实记录行；
- N5 自动刷新后，如果状态集合发生变化，会重建筛选控件；
- 当前筛选无结果时显示明确空状态；
- 不改写 N5 的接口调用和业务数据对象；
- 不使用 LocalStorage / SessionStorage。

## 与详情上下文衔接

N19 的 `listContext()` 现在优先读取 `XHFrontendNextN20.visibleBusinessNos(info)`。

因此详情里的：

- 上一条；
- 下一条；
- “当前已加载列表第 X / Y 条”提示；

都会基于**当前搜索 / 状态 / 排序后的可见结果顺序**，不会跳出当前筛选条件。

如果某个详情深链本身不满足当前筛选条件，详情仍可按 N18 的真实业务编号打开，但 N19 不会伪造它在筛选结果中的序号，也不会提供错误的上一条 / 下一条。

## 与公开业务记录衔接

N20 会把当前安全视图参数附加到 N19 的 `/next/record/{businessNo}` 链接上，同时保留 N19 原有严格来源标记：

- `from=account-requests`
- `from=account-orders`

N13 的账号返回逻辑只复制：

- `q`
- `status`
- `sort`

并继续只接受上面两个 `from` 枚举。这样从公开业务记录返回账号详情时，可以恢复原搜索 / 筛选 / 排序上下文，同时不会引入任意返回 URL。

## 安全和业务边界

N20 runtime：

- 不调用 `fetch`；
- 不新增任何 `/api/...` 请求；
- 不新增 POST / PATCH / PUT / DELETE；
- 不使用客户端持久化存储；
- 不修改支付、退款、售后、订单状态；
- 不放宽账号鉴权；
- 只消费 N5 当前 Session 已授权读取的账号数据。

账号和业务记录页继续保持 `noindex,nofollow`。

## 契约检查

新增：

`FrontendNextN20AccountListViewStateContractTest`

用于静态锁定：

- Preview Controller 注入和内容指纹；
- 已加载 N5 数据范围内的筛选 / 排序；
- URL 参数白名单；
- N19 按筛选结果进行详情前后导航；
- N13 回跳只携带白名单视图参数；
- N20 不新增网络请求、存储或业务写操作。

本阶段没有执行 Maven / Playwright，因此不宣称运行时测试已通过；后续总联调阶段再集中执行真实浏览器和后端测试。

## 发布状态

- 仅开发分支；
- 不部署；
- 不合并 main；
- 不修改 `app.frontend-next.default-enabled=false`；
- 不切正式前台流量。
