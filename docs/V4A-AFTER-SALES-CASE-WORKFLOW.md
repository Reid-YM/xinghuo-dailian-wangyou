# V4A 后台售后处理记录

V70 保存客户提交的售后流水。V4A 增加独立处理状态，不改写客户原文、不触发退款或支付，也不改变前台回显。

## 员工接口

- `GET /api/admin/xingwei/business/orders/{businessNo}/after-sales/records`：要求 `order:view` 且订单在当前数据范围内。返回该订单的流水及处理状态。未处理流水显示 `OPEN`、版本 `0`。
- `PUT /api/admin/xingwei/business/orders/{businessNo}/after-sales/records/{recordId}`：要求 `order:manage`、员工身份及订单数据范围。请求体示例：`{"status":"IN_PROGRESS","handlingNote":"已联系客户核对进度","expectedVersion":0}`。
- 允许 `OPEN → IN_PROGRESS / RESOLVED`、`IN_PROGRESS → RESOLVED`、`RESOLVED → IN_PROGRESS`；同状态写入会被拒绝。每次处理必须填写 1–600 字说明。成功后版本递增并写入审计日志；过期版本返回 `409`。记录不属于该订单返回 `404`。

管理后台现有售后队列提供业务编号，员工可据此读取具体流水。处理说明仅通过后台接口返回，不写入客户可见的售后历史。

## 迁移与回滚

`V71` 仅创建新表，并以 V70 流水 ID 为外键；不扫描或改写历史订单。发布时旧版 8080 可继续读写 V70，候选版 18080/8081 可读写新表。回滚旧 JAR 后客户提交仍可用，但旧版不会展示新处理状态；再次升级后状态恢复。数据库表不随 JAR 回滚自动删除。

验证：Java/Maven CI 覆盖权限、数据范围、版本冲突、跨订单隔离、状态流转、审计和迁移文件哈希。生产发布仍使用 `deploy/production`、`.deploy-request` 以及 18080/8081/8080 门禁。
