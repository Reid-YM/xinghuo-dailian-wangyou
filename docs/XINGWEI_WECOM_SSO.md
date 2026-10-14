# 星维企业微信自建应用免登录

本接入只在新版星维后台新增入口。企业微信返回的 UserID 必须预先绑定到现有 `admin_users` 的星维员工档案；不会自动创建账号、分配角色或改变旧版后台登录流程。

## 配置和入口

- 服务端配置 `WECOM_CORP_ID`、`WECOM_APP_SECRET`、`WECOM_AGENT_ID`，以及公网 HTTPS `APP_BASE_URL`。
- 在企业微信自建应用设置中，将 `APP_BASE_URL` 的域名设为可信域名，并把应用主页设置为 `https://你的域名/admin/xingwei/wecom/login`。
- `WECOM_SSO_ENABLED` 默认 `false`。启用前必须设置 `SESSION_COOKIE_SECURE=true`，并确认应用可见范围、可信域名、账号绑定和权限；未设置安全 Cookie 时登录入口保持关闭。
- OAuth 回调是 `/admin/xingwei/wecom/callback`，使用 `snsapi_base`，仅取得企业成员 UserID。一次性随机 `state` 在服务端 Session 中保存，五分钟过期。回调使用现有 `WECOM_APP_SECRET` 换取应用 access token 并查询 UserID；凭据与 token 不进入页面或审计记录。

## 绑定与权限

管理员先为目标 `admin_users` 创建星维员工档案，并分配星维角色。然后以现有后台管理员 Session 和 CSRF token 调用 `PUT /admin/xingwei/wecom/bindings/{adminUserId}`，请求体如 `{"wecomUserId":"employee_id"}`。传 `null` 可解除绑定；可用同路径的 `GET` 复核结果。绑定只允许 `ADMIN`，UserID 在数据库中唯一。请从企业微信已核实的成员资料中复制 UserID，不要按姓名猜测。

登录仅接受 `ACTIVE` 档案、启用的 `ADMIN` / `OPERATOR` 账号；`OPERATOR` 还必须有启用的星维角色。成功后建立标准 Spring Security Session，跳转到 `/admin/xingwei/work`。现有 `XingweiPermissionGuard` 继续决定新版接口权限和数据范围。通过此入口建立的 Session 会在每次请求复核绑定、账号、档案、角色及开关状态；失效即注销。

## 发布边界

V68 只增加可空 `wecom_user_id` 和唯一索引，不回填任何身份。旧版本忽略新列。发布前仍需按正式流程检查当前数据库 Flyway 版本、旧版本兼容性和候选实例行为；生产发布、V68 实际执行、配置密钥及启用登录分别经过明确授权和验证。
