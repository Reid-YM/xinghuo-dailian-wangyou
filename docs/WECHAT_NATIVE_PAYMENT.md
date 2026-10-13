# 微信支付 Native + 小程序支付接入与上线清单

本功能默认关闭。后台只有在客服确认最终报价、订单已绑定客户账号后，才能建立支付单。客户可以在网站「我的订单」使用 Native 二维码，也可以在已关联的“星维网络”小程序内发起微信支付。付款状态只以微信支付验签通知或主动查单为准。

## 1. 先核对生产服务（只读）

```bash
sudo systemctl show xinghuo-v2.service -p User -p Group -p WorkingDirectory -p ExecStart -p EnvironmentFiles
```

核对当前 `User`、`Group`、`WorkingDirectory` 与 `EnvironmentFiles`。下面示例假设 `User=Group=xinghuo`、环境文件为 `/opt/xinghuo-v2/.env`；若实际不同，按真实结果调整。配置生产凭据或部署前须取得项目负责人明确确认。

## 2. 证书文件

把用户本机保存的 `apiclient_key.pem` 与 `pub_key.pem` 安全上传至服务器临时目录，再用 `sudo install` 安装；不要经聊天、GitHub、日志或命令参数传输私钥正文。服务器目标目录：

```text
/etc/xinghuo-wechat-pay/apiclient_key.pem
/etc/xinghuo-wechat-pay/pub_key.pem
```

目录建议 `root:xinghuo 0750`，两个文件 `root:xinghuo 0640`。应用用户需要读文件，其他用户不应读。安装后立即删除临时上传副本。公钥文件是微信支付公钥，不能误用商户 API 证书公钥。

## 3. 环境变量

生产发布脚本让正式服务、18080 预发布和 8081 金丝雀共同读取 `/opt/xinghuo-v2/.env`。先保持支付关闭；真实 `WECHAT_PAY_API_V3_KEY` 只在服务器本地填写，不写进仓库或聊天：

```dotenv
WECHAT_PAY_ENABLED=false
WECHAT_PAY_MERCHANT_ID=1118501429
WECHAT_PAY_APP_ID=<必须与用于小程序支付的星维网络小程序 AppID 一致>
WECHAT_PAY_MERCHANT_SERIAL=<商户证书序列号>
WECHAT_PAY_PRIVATE_KEY_PATH=/etc/xinghuo-wechat-pay/apiclient_key.pem
WECHAT_PAY_PUBLIC_KEY_PATH=/etc/xinghuo-wechat-pay/pub_key.pem
WECHAT_PAY_PUBLIC_KEY_ID=<微信支付公钥 ID>
WECHAT_PAY_API_V3_KEY=<在服务器本地填写 32 字节密钥>
WECHAT_PAY_NOTIFY_URL=https://wydlgf.cn/api/wechat-pay/notify
```

小程序支付还要求 `WECHAT_MINI_APP_ID` 与 `WECHAT_PAY_APP_ID` 一致，并且该 AppID 已在微信支付商户侧完成合法绑定。代码会在小程序支付接口再次校验 AppID 一致性；不一致时拒绝预下单，不影响网站其他功能。

## 4. 支付通道与状态原则

- 客服先确认金额并建立支付单；浏览器和小程序都不能提交收款金额。
- 同一个支付单第一次预下单锁定通道为 `NATIVE` 或 `MINI_PROGRAM`。不要用同一个 `out_trade_no` 混用两种交易类型。
- 若用户需要切换通道，客服先关闭旧支付单，再按最终金额重建。
- 小程序只使用服务端签发的 `wx.requestPayment` 参数；AppSecret、APIv3 密钥和私钥永不进入客户端。
- `wx.requestPayment success` 不能作为到账依据。只有验签成功的微信回调或服务端主动查单得到 SUCCESS 才会把支付单写为 `CAPTURED`。

## 5. 代码发布与验证

支付变更须走独立分支、PR、CI、主线；生产部署需另行明确授权，按 `AGENTS.md` 使用受控发布流程。V66 为 Native 二维码元数据；V68 为小程序支付增加可空元数据与通道字段，并只对已有 Native `code_url` 做可逆识别回填。部署前仍需核对生产数据库迁移状态及评审清单。

上线验证顺序：

1. 确认服务已启动、凭据文件可由服务用户读取、日志无私钥内容或配置错误。
2. 用已绑定账号的测试订单，在后台录入已确认金额。未绑定账号的订单应拒绝创建支付单。
3. Native 路径：客户在网站生成二维码，核对金额、收款商户与订单号后小额实付。
4. 小程序路径：同一测试账号的新支付单在“星维网络”小程序中显示，点击微信支付，核对金额与商户；客户端返回后仍等待服务端确认。
5. 核对回调返回 200、支付单状态 `CAPTURED`、流水只入库一次；重复回调应保持单次入账。
6. 测试 Native/小程序通道互斥、取消、重复点击与超时关闭。回调丢失时，定时任务每分钟主动查单，不能凭客户端状态自行判定付款成功。
7. 对账完成前不要删除或改写已支付记录。若发现收款异常，先将 `WECHAT_PAY_ENABLED=false` 停止新建/预下单，再人工核对已经收款的记录。

**不要用真实商户凭据跑本地测试，也不要在聊天中发送 APIv3 密钥或 PEM 内容。**
