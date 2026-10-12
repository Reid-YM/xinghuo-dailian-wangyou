# 星维管理后台迁移方案

## 目标

以 `wbr664393425-lgtm/scaffold-master` 的后台信息架构、组织权限和交互设计作为参考，在不推倒现有线上系统的前提下，将星火网游现有后台逐步升级为“星维网络服务工作室管理后台”。

## 核心原则

1. 不直接替换当前 `/admin`，新版入口使用 `/admin/xingwei` 并行运行。
2. 不引入第二套认证。继续使用现有 Spring Security Session、管理员账号、CSRF 和角色边界。
3. 不把 scaffold-master 的 MyBatis/JWT 数据层直接合并进现有 JPA/Flyway 项目。
4. 数据库变更只通过新的 Flyway migration 进入，并保持向后兼容。
5. 所有生产变更继续走 GitHub Pull Request、CI、生产部署门禁和可回滚 release。
6. 固定组织架构、中心编号和正式岗位编码以工作室唯一正式版文件为基准，不由后台随意改名或删除。
7. 先用“影子权限”记录角色/权限配置，确认行为和数据范围稳定后再逐步接管真实接口授权，避免管理员锁死。

## 阶段

### V1：并行管理入口（已上线）

- `/admin/xingwei` 新版后台入口。
- 复用现有管理员登录与退出。
- 复用旧后台的询价、订单、服务、SEO、审计等功能入口。
- 新增后台健康检查 `/api/admin/xingwei/health`。
- 桌面端固定侧栏，移动端抽屉导航。
- 模块快速搜索。
- 不改数据库，不改变现有前台和旧后台行为。

生产版本通过 PR #362、PR CI、main CI 和生产零停机部署门禁上线。

### V2A：组织、岗位、员工档案与影子 RBAC（当前阶段）

Flyway migration：`V63__xingwei_org_rbac_foundation.sql`。

新增表：

- `xw_departments`：固定七大中心。
- `xw_positions`：155 个正式岗位词库。
- `xw_admin_profiles`：现有 `admin_users` 的员工档案扩展。
- `xw_rbac_roles`：影子角色。
- `xw_permissions`：细粒度权限点。
- `xw_admin_roles`：后台账号与影子角色关系。
- `xw_role_permissions`：影子角色与权限关系。

固定七大中心：

1. `01 管理层`
2. `02 运营中心`
3. `03 客服与订单中心`
4. `04 游戏业务中心`
5. `05 技术产品中心`
6. `06 市场与渠道中心`
7. `07 综合支持`

岗位使用正式合同索引中的 `XW-LC-XX-XXX` 编码，共 155 个。后台只允许员工档案选择这些标准岗位，避免自由文本造成同岗多名或组织数据漂移。

影子角色首批为七个中心角色：

- `XW_MANAGEMENT`
- `XW_OPERATIONS`
- `XW_CUSTOMER_ORDER`
- `XW_GAME_BUSINESS`
- `XW_TECH_PRODUCT`
- `XW_MARKET_CHANNEL`
- `XW_GENERAL_SUPPORT`

V2A 权限规则：

- `/admin/**`、`/api/admin/**` 的真实 Spring Security 授权仍由现有 `ADMIN` / `OPERATOR` 控制。
- 读取组织快照沿用现有后台访问边界。
- 员工档案、员工影子角色、角色权限修改额外要求旧体系 `ADMIN`。
- `XW_MANAGEMENT` 是影子权限安全兜底角色，V2A 不允许在页面修改它的权限。
- 所有 V2A 写操作进入现有审计日志。
- V2A 不自动给现有管理员账号分配中心、岗位或影子角色。
- V2A 不修改 `admin_users` 表结构、不重写现有 role、不创建第二套账号密码。

生产迁移安全：

- V63 采用纯新增表/基础数据方式。
- V63 的精确 Git blob SHA 必须写入 `docs/REVIEWED_FLYWAY_MIGRATIONS.txt`。
- `ReviewedFlywayMigrationContractTest` 会锁定审核后的 V63 blob；内容一旦变化必须重新审核。
- 生产部署脚本继续通过 `migration_has_exact_review` 拒绝未审核 migration。

### V2B：权限执行与数据范围（下一阶段）

V2A 稳定后再逐步启用：

1. 新版后台菜单按影子权限显示/隐藏，但 legacy `ADMIN` 永远保留完整兜底入口。
2. 先给只读接口加入权限判定，再给低风险写接口加入权限判定。
3. 订单、询价等业务接口按“全部 / 本中心 / 本人”逐步接入数据范围。
4. 每次启用一个权限点都增加低权限账号回归测试，验证真实接口 403，而不是只隐藏按钮。
5. 在确认无锁死风险前，不删除 `ADMIN` / `OPERATOR` 旧角色模型。

### V3：业务迁移

优先级：

1. 询价与客户线索。
2. 订单与履约。
3. 派单与员工工作台。
4. 服务项目与游戏资料。
5. 售后与异常单。

每个模块迁移时采用“新页面调用现有服务层/仓储层”的方式，避免重复建表和双写。

### V4：渠道与微信能力

- 网站微信登录与小程序账号互通状态。
- 微信支付订单、回调、退款和异常对账。
- 淘宝/闲鱼运营任务与状态汇总。
- 官网与渠道的服务状态统一配置。

## scaffold-master 能力映射

| scaffold-master | 星维落地方式 |
| --- | --- |
| 用户/角色/部门/岗位 | 参考模型，在现有 JPA/JdbcTemplate/Flyway 内重新实现 |
| 菜单/按钮权限 | 先影子记录，V2B 再叠加到现有 ADMIN/OPERATOR 安全边界 |
| 数据范围 | 用服务层/JPA/JdbcTemplate 查询条件实现，不直接移植 MyBatis 拦截逻辑 |
| 登录/操作日志 | 复用现有审计表，V2A 的组织/RBAC 写操作已经接入 |
| 字典/参数 | 评估与现有站点配置是否重复后再引入 |
| 代码生成 | 仅作为开发工具评估，不进入当前生产链路 |
| Vue 3 前端 | 视觉/交互作为参考；当前继续使用 Thymeleaf + 原生 JS，降低迁移面 |

## 发布和回滚

V2A 开发分支：`feat/xingwei-admin-v2a-org-rbac`

生产流程继续遵守仓库现有规则：

1. 功能分支提交。
2. Pull Request 到 `main`。
3. PR CI 全部通过（Maven + Chromium/WebKit）。
4. 合并到 `main` 后再次通过 main CI。
5. migration 精确审核门禁通过。
6. 才允许通过 `deploy/production` 的 `.deploy-request` 触发生产部署。
7. 生产预发布、canary、8080 冒烟任一失败时停止切换或使用现有 release/symlink 回滚机制。

## V2A 验收项

- V63 只新增 `xw_*` 表，不修改 `admin_users`。
- 七大中心名称与编号固定。
- 155 个正式岗位编码完整且唯一。
- `/api/admin/xingwei/org/snapshot` 可读取组织快照。
- 旧 `ADMIN` / `OPERATOR` 仍是实际后台访问门槛。
- V2A 三类写接口都必须为 legacy `ADMIN`。
- `XW_MANAGEMENT` 影子角色不能被页面修改权限。
- 员工档案只能绑定固定中心与正式岗位，且岗位必须属于所选中心。
- 员工档案、角色、权限变更进入现有操作审计。
- `/admin/xingwei` PC 与手机布局无横向溢出。
- 组织 KPI、岗位筛选、员工配置、角色权限弹窗通过浏览器回归测试。
- 旧 `/admin`、前台、微信、小程序和现有订单流程保持原行为。
