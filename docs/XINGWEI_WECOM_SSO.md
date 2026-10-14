# 星维企业微信自建应用免登录

本接入只在新版星维后台新增入口。企业微信返回的 UserID 会自动登记到星维员工档案；自动角色授权由独立开关控制，默认关闭。旧版后台的密码登录流程不变。

## 配置和入口

- 服务端配置 `WECOM_CORP_ID`、`WECOM_APP_SECRET`、`WECOM_AGENT_ID`，以及公网 HTTPS `APP_BASE_URL`。
- 在企业微信自建应用设置中，将 `APP_BASE_URL` 的域名设为可信域名，并把应用主页设置为 `https://你的域名/admin/xingwei/wecom/login`。
- `WECOM_SSO_ENABLED` 默认 `false`。启用前必须设置 `SESSION_COOKIE_SECURE=true`，并确认应用可见范围、可信域名、账号绑定和权限；未设置安全 Cookie 时登录入口保持关闭。
- `WECOM_AUTO_ROLE_ENABLED` 默认 `false`，与免登录开关独立。保持关闭时，新员工只登记为待分配，不会因企业微信职务获得后台角色；已有手动角色的员工仍可免登录。开启前需单独审核生产角色权限和试用人员。
- OAuth 回调是 `/admin/xingwei/wecom/callback`，使用 `snsapi_base`，仅取得企业成员 UserID。一次性随机 `state` 在服务端 Session 中保存，五分钟过期。回调使用现有 `WECOM_APP_SECRET` 换取应用 access token 并查询 UserID；凭据与 token 不进入页面或审计记录。

## 绑定与权限

员工首次从企业微信应用进入时，系统使用 OAuth 返回的 UserID 自动建立 `admin_users` 与 `xw_admin_profiles` 记录，账号使用随机不可知密码，档案为 `ACTIVE`。仅在独立授权开关开启时，系统再读取企业微信实时“职务”并尝试自动授权。本轮仅批准 `在线客服 → XW_CUSTOMER_ORDER`；还必须与 V63 内置岗位名称严格同名、全库唯一、岗位启用，且现有角色启用并保持 `DEPARTMENT` 数据范围。其他所有职务，包括管理层与服务器运维岗位，均待分配，不能自动进入业务工作台。此映射不使用企业微信部门编号，也不按关键字或相似名称推断。扩充允许列表需要单独审查该角色的实际权限。

自动角色记录在 V69 的 `xw_wecom_auto_roles` 中。员工再次进入时会重新读取职务；职务变更为其他已映射岗位则更新角色，变更为未知职务则撤销原自动角色并回到待分配。管理员手动修改角色，或把员工改到其他星维岗位时，自动角色及追踪标记会清除，之后不会覆盖管理员的配置。已经有手动角色的账号也不会被自动映射改写。管理员可在星维组织与权限页面核对企业微信 UserID、角色和中心。

已有后台账号可由管理员预先显式绑定：以现有后台管理员 Session 和 CSRF token 调用 `PUT /admin/xingwei/wecom/bindings/{adminUserId}`，请求体如 `{"wecomUserId":"employee_id"}`。传 `null` 可解除绑定；可用同路径的 `GET` 复核结果。绑定只允许 `ADMIN`，UserID 在数据库中唯一。请从企业微信已核实的成员资料中复制 UserID，不要按姓名猜测。若员工已经自动登记，管理员应先核对并处理重复档案，避免将同一身份绑定给两个账号。

登录仅接受 `ACTIVE` 档案、启用的 `ADMIN` / `OPERATOR` 账号；`OPERATOR` 还必须有启用的星维角色。成功后建立标准 Spring Security Session，跳转到 `/admin/xingwei/work`。现有 `XingweiPermissionGuard` 继续决定新版接口权限和数据范围。通过此入口建立的 Session 只允许访问新版星维后台，不允许访问旧版 `/admin/**` 与 `/api/admin/**`；每次请求复核绑定、账号、档案、角色及开关状态，失效即注销。

## 发布边界

本次使用已经发布的 V68 `wecom_user_id` 唯一索引，并新增 V69 自动角色追踪表；V69 仅新增表，不修改既有业务表数据或服务端凭据。发布前仍需按正式流程完成 PR、CI、候选实例冒烟和生产部署授权。自动角色开关保持默认关闭；后续启用前需核对 V63 岗位词库与现有角色权限，再让少量员工试用。
