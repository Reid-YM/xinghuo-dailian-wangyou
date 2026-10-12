# 星维管理后台 V2C：数据范围执行

## 目标

在 V2B 混合权限执行基础上，把角色的 `data_scope` 从展示字段升级为后端真实数据边界。

V2C 第一批只作用于新版星维组织/员工接口，不改变旧 `/admin` 的 ADMIN / OPERATOR 安全边界，也不修改询价、订单等业务表结构。

## 范围模型

- `ALL`：可访问全部员工数据。
- `DEPARTMENT`：可访问本人所属中心的员工；本人未分配中心时安全降级为仅本人。
- `SELF`：仅可访问本人记录。

有效范围按账号所有启用角色取最宽范围：`ALL > DEPARTMENT > SELF`。

## 兼容策略

- legacy `ADMIN`：`LEGACY_ADMIN_OVERRIDE`，始终 `ALL`，继续作为防锁死兜底。
- 未配置星维角色的 legacy `OPERATOR`：`LEGACY_OPERATOR_COMPAT`，继续保留旧兼容行为和全量查看范围，但仍不能使用 V2B/V2C 管理写权限。
- 已配置星维角色的 `OPERATOR`：`SHADOW_RBAC`，同时执行权限点与 `data_scope`。

## 本阶段实际执行位置

### 读取

`GET /api/admin/xingwei/org/snapshot`

当拥有 `employee:view` 时，返回的 `employees` 会在后端按数据范围裁剪；`counts.employees` 与 `counts.assignedEmployees` 也基于裁剪后的结果重新计算。

响应额外暴露：

- `dataScope`
- `scopeDepartmentCode`
- `scopeAdminUserId`

这些字段只用于新版后台展示当前安全上下文，不改变旧后台行为。

### 写入

以下写操作在权限点校验后继续执行数据范围校验：

- `PUT /api/admin/xingwei/org/employees/{id}/profile`
- `PUT /api/admin/xingwei/org/employees/{id}/roles`

越过数据范围的请求返回 HTTP 403。`DEPARTMENT` 范围的档案写入还会校验变更后的中心，避免通过移动员工到其他中心绕过边界。

角色权限模板本身仍是全局配置，`PUT /roles/{roleCode}/permissions` 继续由 `rbac:manage` 控制，不按员工数据范围裁剪。

## 前端

- 顶部权限徽章显示当前 `ALL / DEPARTMENT / SELF` 范围。
- 组织工作台显示 V2C 数据范围。
- `DEPARTMENT` 范围编辑员工档案时，只提供当前中心作为中心选项；真正的安全校验仍以后端为准。
- 角色列表统一显示“全部数据 / 本中心 / 仅本人”。

## 数据库

V2C 不新增 Flyway migration，继续使用 V63 已存在的：

- `xw_rbac_roles.data_scope`
- `xw_admin_profiles.department_id`
- `xw_admin_roles`

因此本阶段可以通过现有零停机发布链路直接回滚 JAR，不涉及数据库回滚。

## 测试要求

必须覆盖：

1. ADMIN 永远为 `ALL`。
2. 未分配星维角色的 OPERATOR 保留兼容范围。
3. `DEPARTMENT` 账号可访问同中心员工、拒绝其他中心员工。
4. `SELF` 账号只能访问本人。
5. 跨中心员工档案/角色写入被拒绝。
6. Chromium + WebKit 现有后台 UI 回归继续通过。

## 下一阶段 V2D

在订单/询价具备明确“负责人/所属中心”字段后，把同一 `AccessSnapshot` 数据范围复用到：

- 询价线索可见范围
- 订单可见范围
- 派单候选员工范围
- 订单进度写权限

在业务表缺乏稳定归属字段之前，不通过用户名、备注或前端参数猜测数据归属。
