# 星维管理后台 V2D：询价 / 订单业务归属与数据范围

## 目标

V2D 将 V2C 已上线的 `ALL / DEPARTMENT / SELF` 数据范围扩展到新版星维询价与订单接口，同时保持旧 `/admin` 与 `/api/admin/**` 历史业务接口不变。

## 设计原则

1. 不修改 `quotes`、`customer_orders`、`admin_users` 表结构。
2. 新增 `xw_business_ownership`，以统一 `business_no` 作为主键保存业务所属中心与负责人。
3. 询价转订单继续沿用同一个 `business_no`，因此无需在转单时复制归属数据。
4. 未归属业务仅对 `ALL` 范围可见；`DEPARTMENT` 与 `SELF` 默认不可见，避免跨中心数据泄漏。
5. `DEPARTMENT` 只能查看和调整本中心已归属业务；`SELF` 只能查看和调整本人负责业务。
6. 业务归属修改进入现有 `AuditService`，动作分别记录为 `XW_QUOTE_OWNERSHIP_UPDATE` 与 `XW_ORDER_OWNERSHIP_UPDATE`。
7. 客户侧公开查询、账户中心和旧后台 API 不接入 V2D 数据范围，避免一次性切换造成兼容风险。

## 权限

- 询价列表：`quote:view`
- 询价归属：`quote:manage`
- 订单列表：`order:view`
- 订单归属 / 派单：`order:assign`

继续复用 V63 已存在权限点，不新增第二套权限命名。

## 新版接口

- `GET /api/admin/xingwei/business/quotes`
- `GET /api/admin/xingwei/business/orders`
- `GET /api/admin/xingwei/business/ownership-candidates`
- `PUT /api/admin/xingwei/business/quotes/{businessNo}/ownership`
- `PUT /api/admin/xingwei/business/orders/{businessNo}/ownership`

列表在数据库层先执行当前账号数据范围，再按时间倒序最多返回 100 条，避免“先取全局前 100 再过滤”导致本中心较早业务被错误挤出。

## 归属规则

- `ALL`：可查看未归属和所有已归属业务，可分配到任意启用中心 / 在岗员工。
- `DEPARTMENT`：只可查看本中心已归属业务，可在本中心内改派负责人或改为中心队列；不可认领未归属业务。
- `SELF`：只可查看本人负责业务，只能继续分配给本人；不能改为中心队列。
- 负责人必须是启用后台账号，并已配置 `ACTIVE` 员工档案与有效中心。
- 当同时提交负责人和中心时，中心必须与负责人当前员工档案一致。

## 数据库发布安全

Flyway migration：`V64__xingwei_business_ownership.sql`。

该 migration 仅创建新增表和外键，不修改任何现有业务表。其精确 Git blob SHA 写入 `docs/REVIEWED_FLYWAY_MIGRATIONS.txt`，并由 `ReviewedFlywayMigrationContractTest` 锁定；生产自动发布仍必须通过精确 migration 审核门禁。

## 回滚边界

应用回滚时旧 `/admin` 与历史 API 不依赖 V64 表，因此可直接回滚 JAR。V64 为纯新增表，不要求数据库反向删除；保留表不会影响上一版本运行。
