# 星维管理后台迁移方案

> 以下记录原阶段设计；当前默认入口和完整替换以 [完整迁移计划](development/2026-10-16-complete-migration-plan.md) 为准。

## 目标

以 `wbr664393425-lgtm/scaffold-master` 的后台信息架构、组织权限和交互设计作为参考，在不推倒现有线上系统的前提下，将星火网游现有后台逐步升级为“星维网络服务工作室管理后台”。

## 核心原则

1. 不直接替换当前 `/admin`，新版入口使用 `/admin/xingwei` 并行运行。
2. 不引入第二套认证。继续使用现有 Spring Security Session、管理员账号、CSRF 和 legacy `ADMIN` / `OPERATOR` 外层安全边界。
3. 不把 scaffold-master 的 MyBatis/JWT 数据层直接合并进现有 JPA/Flyway 项目。
4. 数据库变更只通过新的 Flyway migration 进入，并保持向后兼容。
5. 所有生产变更继续走 GitHub Pull Request、CI、生产部署门禁和可回滚 release。
6. 固定组织架构、中心编号和正式岗位编码以工作室唯一正式版文件为基准，不由后台随意改名或删除。
7. 权限采用渐进接管：先在新版星维接口启用细粒度 RBAC，旧 `/admin` 继续保留 legacy 权限兜底，避免一次性切换导致管理员锁死。

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

### V2A：组织、岗位、员工档案与 RBAC 数据底座（已上线）

生产版本通过 PR #363、PR CI、main CI、精确 migration 审核与生产零停机部署门禁上线。

Flyway migration：`V63__xingwei_org_rbac_foundation.sql`。

新增表：

- `xw_departments`：固定七大中心。
- `xw_positions`：155 个正式岗位词库。
- `xw_admin_profiles`：现有 `admin_users` 的员工档案扩展。
- `xw_rbac_roles`：星维角色。
- `xw_permissions`：细粒度权限点。
- `xw_admin_roles`：后台账号与星维角色关系。
- `xw_role_permissions`：星维角色与权限关系。

固定七大中心：

1. `01 管理层`
2. `02 运营中心`
3. `03 客服与订单中心`
4. `04 游戏业务中心`
5. `05 技术产品中心`
6. `06 市场与渠道中心`
7. `07 综合支持`

岗位使用正式合同索引中的 `XW-LC-XX-XXX` 编码，共 155 个。后台只允许员工档案选择这些标准岗位，避免自由文本造成同岗多名或组织数据漂移。

首批七个中心角色：

- `XW_MANAGEMENT`
- `XW_OPERATIONS`
- `XW_CUSTOMER_ORDER`
- `XW_GAME_BUSINESS`
- `XW_TECH_PRODUCT`
- `XW_MARKET_CHANNEL`
- `XW_GENERAL_SUPPORT`

V63 采用纯新增表/基础数据方式，不修改 `admin_users`。其精确 Git blob SHA 已写入 `docs/REVIEWED_FLYWAY_MIGRATIONS.txt`，并由 `ReviewedFlywayMigrationContractTest` 和生产 `migration_has_exact_review` 门禁锁定。

### V2B：新版星维权限执行（当前阶段）

V2B 不新增数据库 migration，直接使用 V2A 已上线的角色与权限数据。

#### 防锁死混合权限模型

`XingweiPermissionGuard` 负责新版星维接口的有效权限计算：

1. **legacy ADMIN**：`LEGACY_ADMIN_OVERRIDE`，始终保留新版星维全权限。即使星维角色配置错误，也不会失去恢复入口。
2. **未分配星维角色的 legacy OPERATOR**：`LEGACY_OPERATOR_COMPAT`，保留新版星维查看类权限，但不能执行员工、角色等写操作。
3. **已明确分配星维角色的 OPERATOR**：`SHADOW_RBAC`，新版星维接口仅按角色所拥有的 `xw_role_permissions` 精确授权。
4. **未知、匿名或不属于 ADMIN/OPERATOR 的主体**：`DENIED`。

`manage` 自动包含同模块 `view`。例如拥有 `employee:manage` 时无需再单独配置 `employee:view`；但 `employee:manage` 不会自动获得 `rbac:manage`。

#### V2B 当前接管范围

只接管新版星维后台自身能力，不修改旧 `/admin` 历史接口：

- `GET /api/admin/xingwei/access`：返回当前账号在新版星维后台的有效模式、角色和权限点。
- `GET /api/admin/xingwei/org/snapshot`：要求至少拥有 `org:view` / `employee:view` / `rbac:view` 之一，并按实际权限裁剪组织、员工和 RBAC 数据后再返回。
- 员工档案写入：`employee:manage`。
- 员工角色分配：`rbac:manage`。
- 角色权限调整：`rbac:manage`。

所有写操作仍进入现有 `AuditService`。`XW_MANAGEMENT` 继续作为安全兜底角色，其权限在页面上不可编辑。

#### 前端权限同步

`/admin/xingwei` 会读取 `/api/admin/xingwei/access`：

- 模块列表按查看权限过滤。
- 搜索结果不会重新显示无权限模块。
- `manage` 对应的模块仍可正常显示，因为 `manage` 自动包含 `view`。
- 员工“配置”按钮仅在 `employee:manage` 或 `rbac:manage` 时出现。
- 角色权限按钮仅在 `rbac:manage` 时出现。
- 员工档案与角色写入分离：只有对应权限的部分才会提交，避免部分保存后再因另一接口 403 而显示整体失败。
- 权限状态读取失败时，新版受控模块默认隐藏（fail closed）。

#### 旧后台兼容边界

V2B **不修改 `SecurityConfig`**。因此：

- `/admin/**`、`/api/admin/**` 仍先经过 legacy `ADMIN` / `OPERATOR` 外层门槛。
- 旧 `/admin` 页面和历史 API 暂时不按星维 RBAC 过滤。
- 新版星维接口在通过外层门槛后，再执行 `XingweiPermissionGuard` 的细粒度授权。
- 后续业务迁移到新版接口时，才逐项接入对应权限和数据范围。

### V3：业务迁移

优先级：

1. 询价与客户线索。
2. 订单与履约。
3. 派单与员工工作台。
4. 服务项目与游戏资料。
5. 售后与异常单。

每个模块迁移时采用“新页面调用现有服务层/仓储层”的方式，避免重复建表和双写。接口迁移完成后再接入星维 RBAC 和“全部 / 本中心 / 本人”数据范围。

### V4：渠道与微信能力

- 网站微信登录与小程序账号互通状态。
- 微信支付订单、回调、退款和异常对账。
- 淘宝/闲鱼运营任务与状态汇总。
- 官网与渠道的服务状态统一配置。

## scaffold-master 能力映射

| scaffold-master | 星维落地方式 |
| --- | --- |
| 用户/角色/部门/岗位 | 参考模型，在现有 JPA/JdbcTemplate/Flyway 内重新实现 |
| 菜单/按钮权限 | V2B 已在新版星维后台启用前后端双层权限过滤 |
| 数据范围 | 后续随 V3 业务接口用服务层/JPA/JdbcTemplate 查询条件实现，不直接移植 MyBatis 拦截逻辑 |
| 登录/操作日志 | 复用现有审计表，组织/RBAC 写操作已接入 |
| 字典/参数 | 评估与现有站点配置是否重复后再引入 |
| 代码生成 | 仅作为开发工具评估，不进入当前生产链路 |
| Vue 3 前端 | 视觉/交互作为参考；当前继续使用 Thymeleaf + 原生 JS，降低迁移面 |

## 发布和回滚

V2B 开发分支：`feat/xingwei-admin-v2b-permission-enforcement`

生产流程继续遵守仓库现有规则：

1. 功能分支提交。
2. Pull Request 到 `main`。
3. PR CI 全部通过（Maven + Chromium/WebKit）。
4. 合并到 `main` 后再次通过 main CI。
5. migration 门禁确认本次无未审核新 migration。
6. 仅通过 `deploy/production` 的 `.deploy-request` 触发生产部署。
7. 生产预发布、canary、8080 冒烟任一失败时停止切换或使用现有 release/symlink 回滚机制。

## V2B 验收项

- legacy ADMIN 始终能访问新版星维全部功能。
- 未分配星维角色的 OPERATOR 能查看但不能执行新版组织/RBAC 写操作。
- 已分配星维角色的 OPERATOR 只获得角色实际权限。
- `manage` 自动包含同模块 `view`，不跨模块扩权。
- 组织快照按 `org / employee / rbac` 权限裁剪敏感数据。
- 新版模块搜索不能绕过权限重新显示被隐藏模块。
- 员工档案与角色分配独立按 `employee:manage` / `rbac:manage` 提交。
- 后端 `@PreAuthorize` 与前端显示规则同时存在，不能仅靠隐藏按钮实现权限。
- `/admin/xingwei` PC 与手机布局继续无横向溢出。
- V63 和既有数据库结构保持不变。
- 旧 `/admin`、前台、微信、小程序、支付和现有订单流程保持原行为。
