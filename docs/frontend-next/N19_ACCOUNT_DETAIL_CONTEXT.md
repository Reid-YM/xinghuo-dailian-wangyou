# N19 账号详情上下文连续性

## 目标

N18 已让询价/订单详情拥有可复制、可刷新、可前进后退的深链。N19 继续补齐详情内部的浏览上下文，避免用户查看某条业务后只能关闭并重新从列表寻找。

本阶段只复用 N5 已加载的真实账号数据、N18 深链路由和 N13 公开业务记录，不新增第二套详情接口或业务数据。

## 已实现

### 1. 当前列表内上一条 / 下一条

`n19-account-detail-context.js` 从现有 `XHFrontendNextN5.state` 读取：

- 询价：`state.quotes.items`
- 订单：`state.orders.items`

只在当前已经加载到浏览器的列表范围内计算上一条与下一条，不为了导航偷偷请求其它页或其它账号数据。

详情上下文会显示“当前已加载列表第 X / N 条”。如果列表刷新后顺序、内容或来源询价关系变化，运行时使用列表签名重新生成按钮状态，避免沿用过期的上一条/下一条目标。

### 2. 返回来源列表

询价详情提供“返回询价列表”，订单详情提供“返回订单列表”。

返回时使用浏览器 History API 和 N18 现有路由协调能力，回到：

- `/next/account/requests`
- `/next/account/orders`

同时清理认证和来源等临时查询参数，不引入新的业务请求。

### 3. 订单来源询价

如果现有订单详情包含 `sourceQuote.businessNo`，并且该来源询价已经存在于当前账号已加载的询价列表中，N19 才显示“来源询价”。

这样可以从订单详情回到对应询价详情，同时避免仅凭服务端返回的陌生编号直接制造一个未经过当前账号列表验证的跳转入口。

### 4. 公开业务记录桥接

详情可以进入现有 N13：

- `/next/record/{businessNo}?from=account-requests`
- `/next/record/{businessNo}?from=account-orders`

N13 仍使用原有公开接口 `GET /api/records/{businessNo}`。N19 本身不新增 `fetch`。

N13 现在只识别两个固定来源值：

- `account-requests`
- `account-orders`

匹配时显示“返回询价详情”或“返回订单详情”，并返回同一业务编号的 N18 深链。任意其它 `from` 值都会被忽略，因此不会形成开放重定向或任意路径回跳。

即使公开记录读取失败，只要来源标记合法，错误页仍保留返回原账号详情的入口。

## 数据与安全边界

N19：

- 不新增 API
- 不调用 `fetch`
- 不新增 POST / PATCH / PUT / DELETE
- 不使用 localStorage / sessionStorage
- 不改变支付、退款、售后或订单状态
- 不读取其它账号业务
- 上一条/下一条只使用当前 N5 已加载列表
- 来源询价只在当前已加载询价中确认后展示
- 业务编号继续使用 N13/N18 相同白名单

N13 的公开记录读取逻辑保持原样，只增加严格枚举的账号详情返回入口。

## 代码范围

- `src/main/resources/static/next/n19-account-detail-context.js`
- `src/main/resources/static/next/n13-record-detail.js`
- `src/main/java/com/xinghuo/web/FrontendNextPreviewController.java`
- `src/test/java/com/xinghuo/web/FrontendNextN19AccountDetailContextContractTest.java`
- `docs/frontend-next/N19_ACCOUNT_DETAIL_CONTEXT.md`
- `docs/frontend-next/README.md`

## 验证

新增 N19 静态契约，覆盖：

- Preview Controller 注入和指纹
- 仅使用已加载询价/订单列表
- 上一条/下一条与列表签名刷新
- 订单来源询价约束
- 公开业务记录 `from` 枚举
- N13 精确返回账号详情
- N19 不新增网络请求、客户端存储或业务写请求

当前阶段未部署、未合并 `main`、未切正式流量。没有 GitHub CI 状态时，不宣称 Maven 或 Playwright 已实际执行通过。
