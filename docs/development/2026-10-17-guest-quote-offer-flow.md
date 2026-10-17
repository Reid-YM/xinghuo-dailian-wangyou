# 2026-10-17 游客询价、正式报价与免注册支付闭环

## 目标

把现有“游客只能提交询价线索”的流程扩展成完整成交链路，同时不污染正式用户账号体系：

1. 游客无需注册即可提交询价；
2. 客服可在星维业务工作台发布正式报价；
3. 游客凭浏览器安全凭证查看并接受报价；
4. 接受报价后自动生成订单；
5. 游客无需注册即可使用微信 Native 扫码支付；
6. 现有注册账号询价、订单与支付流程保持兼容；
7. 历史订单自动归属只使用可靠业务血缘，不再根据手填联系方式猜测账号。

## 客户主体

本阶段明确区分两种客户主体：

- `ACCOUNT`：`customer_orders.customer_account_id` 非空；
- `GUEST`：`customer_orders.guest_access_id` 非空。

游客不会被偷偷创建成 `customer_accounts`。账号注册继续代表长期用户身份；游客 capability 只代表当前浏览器对具体业务的访问权。

## 游客安全模型

### guest_session

浏览器获得 256-bit `SecureRandom` 随机 Token，使用 `XH_GUEST` Cookie 保存：

- HttpOnly；
- host-only，不设置 Domain；
- SameSite=Lax；
- HTTPS 时 Secure；
- 有效期 90 天并在有效会话复用时续期。

数据库只保存 `SHA-256(token)`，不保存明文。

### guest_business_access

一条游客会话可以关联多个询价业务。所有 `/api/guest/business/**` 访问都必须同时满足：

- guest session token hash 命中有效会话；
- business_no 属于该 session 的有效 `guest_business_access`。

因此业务编号本身不是授权凭证。

## 正式报价

新增 `quote_offer`，不把正式金额直接覆盖在 `quotes` 上。

每次重新报价：

- 旧的 `QUOTED` 版本标记为 `CANCELLED`；
- 创建 version+1 的新报价；
- 保留历史金额、服务名称、预计时长、客户说明、报价人、有效期和时间；
- `quotes.status` 进入 `QUOTED`。

游客接受最新有效报价后：

- `quote_offer.status = ACCEPTED`；
- `quotes.status = CONVERTED`；
- 由既有 `OrderService.createFromQuote` 创建订单；
- 订单写入 `guest_access_id`；
- 微信支付开启时自动生成 `payment_order`。

接受操作设计成幂等：重复点击已经接受的报价只返回当前业务状态，不重复创建订单。

## 微信支付

新增 `GuestWeChatPaymentService`，继续复用现有 `payment_order`、微信 Native 客户端、回调验签和定时对账。

游客支付单：

- `user_id = NULL`；
- `guest_access_id = 当前 guest_business_access.id`；
- `business_no` 与订单一致。

V40 原始设计中 `payment_order.user_id` 本来就允许 NULL，因此无需放宽旧字段约束。

游客获取二维码前会再次校验：

- payment 的 guest_access_id；
- order 的 guest_access_id；
- 二者一致；
- provider=WECHAT。

微信回调仍按原支付基础设施验签、核对支付单与金额，并写入 `payment_transaction`，不会因为游客路径降低支付安全要求。

## 订单账号自动恢复修复

`AutomaticOrderAccountLinkService` 调整为：

1. 优先 `customer_orders.source_quote_id -> quotes.id`；
2. 仅 `source_quote_id IS NULL` 的历史订单允许 `business_no` 兜底；
3. 只继承来源 quote 已经存在的 `customer_account_id`；
4. 不再使用匿名询价的手机号、QQ、微信号去匹配账号；
5. 已有订单 owner 永不覆盖；
6. 非空但失效的 source_quote_id 不降级到 business_no 猜测。

这解决了两个问题：

- 历史/生产数据即使 business_no 形态不一致，只要 source_quote_id 正确就能恢复；
- 防止攻击者在匿名询价里填写他人的联系方式，导致订单被错误归属到他人账号。

## 后台入口

`/admin/xingwei/business` 的询价列表新增“发布报价”操作。

API：

`POST /api/admin/xingwei/business/quotes/{businessNo}/offers`

要求：

- `quote:manage`；
- 当前管理员的数据范围能访问该 business_no；
- 询价属于游客且有 guest_business_access；
- 询价未关闭、取消或转单。

## 游客前台入口

游客提交询价后，成功卡片增加：

`查看报价 / 业务详情 -> /record/{businessNo}`

该页面保留已有公开进度内容，同时额外调用受 capability 保护的游客 API。没有正确 `XH_GUEST` Cookie 时，私有报价卡不会显示。

游客 API：

- `GET /api/guest/business/{businessNo}`：读取私有报价/订单/支付状态；
- `POST /api/guest/business/{businessNo}/accept`：接受正式报价；
- `POST /api/guest/business/{businessNo}/payment/native`：生成微信 Native 二维码；
- `GET /api/guest/business/{businessNo}/payment/qr.png`：获取服务端渲染二维码 PNG。

正常 JSON 不返回微信 `code_url`。

## 支付工作台

`XingweiPaymentWorkspaceService` 与前端现在识别：

- `ACCOUNT`；
- `GUEST`；
- `UNBOUND`。

游客订单不再显示“客户账号未绑定所以不能支付”。只要 `guest_access_id` 已经由游客正式确认流程建立，就是合法的可支付客户主体。

## 数据迁移 V78

`V78__guest_quote_offer_flow.sql`：

- create `guest_session`；
- create `guest_business_access`；
- create `quote_offer`；
- alter `customer_orders` add `guest_access_id`；
- alter `payment_order` add `guest_access_id`。

新增外键均采用不破坏既有订单/支付事实的删除策略。

## 回归测试重点

新增/调整测试覆盖：

- source_quote_id 与 business_no 不同时仍按来源询价恢复；
- 历史 source_quote_id 为空才按 business_no 兜底；
- 匿名手填联系方式绝不成为自动账号归属证据；
- 同一游客 session 可以持有多条业务；
- 错误 capability 不能读取他人游客业务；
- 多次报价保留历史、只激活最新版本；
- 接受报价生成 guest-owned order；
- 重复接受不创建第二张订单；
- 支付工作台把 guest-owned order 判定为可支付，且不触发账号猜测。

## 尚未包含在本阶段

后续阶段继续做：

1. 游客注册/登录后，把当前 guest business 一键认领进正式账号；
2. 把所有 customer_account_id / guest_access_id 修改进一步集中到统一 `CustomerBusinessBindingService`；
3. 把后台当前轻量报价交互升级成完整 modal；
4. 清理用户 GET“我的订单”中仍存在的确定性历史修复写操作；
5. 对 V35 遗留历史订单跑专门分类/修复任务；
6. 进一步收紧旧公开 `/api/records/**` 与 `/api/orders/**` 可返回字段。

## 发布边界

本改动必须经过 CI、迁移校验和测试后再合并。合并 PR 不等于生产部署；生产发布仍使用仓库既有 Deploy Production 流程并单独确认。
