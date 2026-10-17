# N18 个人中心业务详情深链

## 目标

N16/N17 已让 Next 个人中心的询价、订单、工具存档和账号设置具备稳定子路由与内部导航，但 N5 的“查看询价 / 查看详情”此前仍只是页内展开状态：刷新页面、复制 URL 或使用浏览器前进后退时，无法恢复到某一条具体业务记录。

N18 只补这个缺口，不新建详情 API、不复制 N5 详情 UI、不改变订单、询价、支付或退款业务逻辑。

## 新增显式预览路由

- `/next/account/requests/{businessNo}`：当前账号的一条询价详情。
- `/next/account/orders/{businessNo}`：当前账号的一条订单详情。

两类路由继续由 `FrontendNextPreviewController` 返回同一份 Frontend Next 文档，沿用账号页面现有 `noindex,nofollow`、Session、权限与缓存策略。

业务编号继续使用 N13 已采用的白名单：

- `XH` + 10 位大写字母/数字；或
- `XH-` + 24 位去除易混淆字符后的业务编号。

非法编号不会用于详情打开。

## 复用现有 N5 详情

N18 不再发送新的业务请求。

详情数据仍完全由 N5 的既有实现读取：

- 询价：`/api/console/quotes/{businessNo}`；
- 订单：`/api/console/orders/{businessNo}`。

N18 只等待 N5 的真实列表完成加载，定位对应业务行，然后触发原本的“查看询价 / 查看详情”按钮。因此详情展示、错误处理、支付记录、售后内容和权限边界仍由既有 N5/N5 Actions 负责。

如果当前账号列表中不存在该业务编号，N18 不伪造详情，只在个人中心给出“未找到记录”的错误提示。

## URL 与浏览器历史

用户从询价列表打开一条记录时，URL 进入：

`/next/account/requests/{businessNo}`

从订单列表打开一条记录时进入：

`/next/account/orders/{businessNo}`

关闭详情会回到对应父路由；浏览器前进/后退会重新打开或关闭详情。

N18 与 N16 的 tab 同步复用同一套机制。直接打开询价详情时，如果 N5 当前停在订单 tab，N18 会临时把地址替换为父路由并调用 N16 的 `applyRoute({ canonicalize: false })`，随后立即恢复详情地址。这样 N16 自己负责无递归地切换 tab，且不会额外污染浏览器历史。

## 认证连续性

未登录直接访问有效详情深链时，N18 会在登录表单出现后把当前详情地址写入安全 `next` 参数。

因此密码登录、微信 OAuth、小程序扫码仍沿用 N15 的统一认证回跳机制，成功后返回原来的询价/订单详情，而不是退回 `/next/account`。

写入 `next` 前会清除 `auth`、`token`、`verifyToken`、`wechat`、已有 `next` 和 `legacy` 等瞬态参数，避免认证参数嵌套传播。

## N17 导航联动

N17 的路由识别扩展为：

- `requests/{businessNo}` 继续高亮“询价记录”；
- `orders/{businessNo}` 继续高亮“订单记录”。

详情页不会误回到“总览”高亮状态。

## 安全与范围

N18：

- 不新增 `fetch`；
- 不新增 POST/PUT/PATCH/DELETE；
- 不使用 localStorage/sessionStorage；
- 不使用 `innerHTML` / `insertAdjacentHTML`；
- 不修改支付、退款、金额确认、企业微信等业务；
- 不更改旧版正式账号中心；
- 不开启 `app.frontend-next.default-enabled`；
- 不部署、不切正式流量。

## 验证

新增 `FrontendNextN18AccountDetailDeepLinkContractTest` 做静态契约检查，覆盖：

- Preview Controller 详情路由与 N18 指纹注入；
- 业务编号白名单；
- N5 现有详情 UI 复用且无新增 fetch；
- 打开/关闭/前进后退的 history 同步；
- 未登录详情深链认证回跳；
- N16 tab 同步不会新增历史条目；
- N17 详情路由导航高亮；
- 无客户端存储、危险 HTML 或新增业务写请求。

当前只声明代码与静态契约已经落入开发分支；未执行 Maven/Playwright 时，不宣称这些测试已实际运行通过。
