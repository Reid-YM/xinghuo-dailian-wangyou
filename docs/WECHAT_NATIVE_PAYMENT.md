# 微信支付 Native 接入与上线清单

本功能默认关闭。后台只有在客服确认最终报价、订单已绑定客户账号后，才能建立支付单。客户进入「我的订单 → 查看详情」生成二维码，付款状态只以微信支付验签通知或主动查单为准。

## 1. 先核对生产服务（只读）

```bash
sudo systemctl show xinghuo-v2.service -p User -p Group -p WorkingDirectory -p ExecStart -p EnvironmentFiles
```

核对当前 `User`、`Group`、`WorkingDirectory` 与 `EnvironmentFiles`。下面示例假设 `User=Group=xinghuo`、环境文件为 `/opt/xinghuo-v2/.env`；若实际不同，按真实结果调整。**配置生产凭据或部署前须取得项目负责人明确确认。**

## 2. 证书文件

把用户本机保存的 `apiclient_key.pem` 与 `pub_key.pem` 安全上传至服务器的临时目录，再用 `sudo install` 安装；不要经聊天、GitHub、日志或命令参数传输私钥正文。服务器目标目录：

```text
/etc/xinghuo-wechat-pay/apiclient_key.pem
/etc/xinghuo-wechat-pay/pub_key.pem
```

目录建议 `root:xinghuo 0750`，两个文件 `root:xinghuo 0640`。应用用户需要读文件，其他用户不应读。安装后立即删除临时上传副本。公钥文件是微信支付公钥，不能误用商户 API 证书公钥。生产服务及 18080/8081 候选实例都使用同一服务用户，需能读到文件。

## 3. 环境变量

生产发布脚本让正式服务、18080 预发布和 8081 金丝雀共同读取 `/opt/xinghuo-v2/.env`。在该文件中追加下列变量，真实 `WECHAT_PAY_API_V3_KEY` 只在服务器本地填写，不写进仓库或聊天：

```dotenv
WECHAT_PAY_ENABLED=true
WECHAT_PAY_MERCHANT_ID=1118501429
WECHAT_PAY_APP_ID=wxcc2af059160a52da
WECHAT_PAY_MERCHANT_SERIAL=2EC106123DD853BB0ECD7C341C9A3917CFE88A01
WECHAT_PAY_PRIVATE_KEY_PATH=/etc/xinghuo-wechat-pay/apiclient_key.pem
WECHAT_PAY_PUBLIC_KEY_PATH=/etc/xinghuo-wechat-pay/pub_key.pem
WECHAT_PAY_PUBLIC_KEY_ID=PUB_KEY_ID_0111185014292026101100191921001400
WECHAT_PAY_API_V3_KEY=<在服务器本地填写 32 字节密钥>
WECHAT_PAY_NOTIFY_URL=https://wydlgf.cn/api/wechat-pay/notify
```

先确认 `https://wydlgf.cn` 是正式收款域名、Nginx 将该路径转发到应用，再启用。环境文件应仅允许 root 和运行服务所需用户读取（例如 `root:xinghuo 0640`），不能提交 Git。不要把整份环境文件复制进工单或聊天。

## 4. 代码发布与验证

支付变更须走独立分支、PR、CI、主线；生产部署需另行明确授权，按 `AGENTS.md` 使用受控发布流程。V66 仅为 `payment_order` 新增两个可空字段，旧版应用可继续运行；部署前仍需核对生产数据库迁移状态及评审清单。候选 18080 和金丝雀 8081 会连接生产数据库并执行 Flyway。

上线验证顺序：

1. 确认服务已启动、凭据文件可由服务用户读取、日志无私钥内容或配置错误。
2. 用已绑定账号的测试订单，在后台录入已确认金额。未绑定账号的订单应拒绝创建支付单。
3. 客户在「我的订单」生成二维码，核对金额、收款商户与订单号后小额实付。
4. 核对回调返回 200、支付单状态 `CAPTURED`、流水只入库一次；重复回调应保持单次入账。
5. 测试取消与超时关闭。回调丢失时，定时任务每分钟主动查单，不能凭网页轮询自行判定付款成功。
6. 对账完成前不要删除或改写已支付记录。若发现收款异常，先将 `WECHAT_PAY_ENABLED=false` 停止新建支付单，再按生产回滚流程处理；已收款记录仍需人工核对。

**不要用真实商户凭据跑本地测试，也不要在聊天中发送 APIv3 密钥或 PEM 内容。**
