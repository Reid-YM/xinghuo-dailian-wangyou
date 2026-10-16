# Frontend Next N5 第二段：业务补充与售后操作

## 目标

在 N5 第一段已经上线的真实询价、订单、进度、售后状态和历史订单认领基础上，把账号中心现有的用户写操作迁入 `/next/account`，形成“查看 → 补充 → 售后 → 追问”的业务闭环。

## 复用的现有后端

- `POST /api/console/quotes/{businessNo}/supplement`
- `POST /api/console/orders/{businessNo}/supplement`
- `POST /api/console/orders/{businessNo}/after-sales`
- `POST /api/console/orders/{businessNo}/after-sales/records/{recordId}/followups`

不新增数据库迁移，不新增第二套售后数据模型。

## 前端能力

### 询价补充

- 用户在询价详情内追加需求说明。
- 最长 1200 字。
- 追加而不是覆盖原内容。

### 订单补充

- 用户在订单详情内补充执行偏好或需要客服留意的信息。
- 最长 1200 字。
- 不改变订单状态和后台进度。

### 提交售后问题

支持服务端已有四类问题：

- `DELIVERY`：交付或进度
- `CANCELLATION`：取消服务
- `REFUND_REVIEW`：退款核对
- `OTHER`：其他问题

问题说明最长 240 字。

### 售后追问

- 针对已有 `recordId` 在原售后记录内补充。
- 最长 240 字。
- 不新建重复工单，继续保留在原记录历史下。

## 安全

- 所有写请求继续携带 `X-Requested-With: fetch`。
- 请求使用当前账号的同源会话，不在浏览器存储账号或业务数据。
- 不使用 `innerHTML`、`localStorage`、`sessionStorage`。
- 页面明确提示不要填写密码、短信验证码或登录凭据。
- 业务归属、长度限制、问题类型和售后 recordId 可见性仍由服务端再次校验。
- 提交成功后刷新 N5 真实业务数据；如果刷新失败，不把已经成功的写请求误报为失败。

## 回退

本阶段为独立 `/next/n5-actions.js` runtime。移除该 runtime 后：

- N5 第一段的询价/订单/进度/历史认领仍可正常使用；
- N4 账号与微信/小程序登录不受影响；
- N3 询价不受影响；
- 旧 `/account` 可继续完整回退；
- 新版后台不受影响。

## 测试

- Java 合约：runtime 顺序、服务端端点、长度限制、账号隔离和前端安全约束。
- Chromium：询价补充写入、请求头、成功后刷新。
- WebKit：订单补充、退款核对类售后提交、指定售后记录追问。
