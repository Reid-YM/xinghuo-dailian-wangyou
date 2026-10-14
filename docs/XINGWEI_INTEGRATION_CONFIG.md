# 星维后台：企业微信与微信支付配置

V3O 新版后台入口：`/admin/xingwei/integrations`。页面仅展示服务器配置的布尔检查结果，不返回配置值、密钥、证书内容或路径。需要 `employee:view` 或 `payment:view`；每个板块还会分别按权限隐藏。

## 企业微信

| 服务器变量 | 用途 | 当前主线能力 |
| --- | --- | --- |
| `WECOM_CORP_ID` | 企业 ID | 通讯录读取 |
| `WECOM_APP_SECRET` | 只读应用密钥 | 通讯录读取 |
| `WECOM_SECRET` | 通讯录写入凭据 | 仅显示是否存在；PR #372 未合并，不启用写入 |

“读取配置齐全”只表示前两项非空。需要进入 `/admin/xingwei/wecom` 实际读取员工和部门，才能确认网络、权限及企业微信接口正常。岗位继续读取 V63 正式数据库。写入能力与真实员工变更需单独验收。

## 微信支付

| 服务器变量 | 配置检查 |
| --- | --- |
| `WECHAT_PAY_ENABLED` | 当前 Native 收款开关状态；默认关闭 |
| `WECHAT_PAY_MERCHANT_ID` | 非空 |
| `WECHAT_PAY_APP_ID` | 非空 |
| `WECHAT_PAY_MERCHANT_SERIAL` | 非空 |
| `WECHAT_PAY_PRIVATE_KEY_PATH` | 绝对路径、普通文件、服务进程可读 |
| `WECHAT_PAY_PUBLIC_KEY_PATH` | 绝对路径、普通文件、服务进程可读 |
| `WECHAT_PAY_PUBLIC_KEY_ID` | 非空 |
| `WECHAT_PAY_API_V3_KEY` | UTF-8 长度为 32 字节 |
| `WECHAT_PAY_NOTIFY_URL` | HTTPS 且以 `/api/wechat-pay/notify` 结尾 |
| `WECHAT_MINI_APP_ID` | 仅用于提示小程序 AppID 是否与支付 AppID 一致 |

配置齐全不代表商户绑定、证书有效、回调可达或支付已成功。当前主线仅有网站 Native 支付代码；V3H PR #384 的小程序支付仍未合并，且需适配实际使用的“星火迷彩追踪器”。网站旧后台保留客服确认最终金额、创建或关闭支付单的已有操作入口。新版工作台不允许直接修改服务器密钥，也不提供切换真实收款的按钮。

任何生产启用继续走 `main` PR、CI、`deploy/production`、`.deploy-request` 与 18080 / 8081 / 8080 门禁。真实小额支付和企业微信写入各自单独验收。
