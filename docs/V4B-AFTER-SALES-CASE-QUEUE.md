# V4B 售后处理队列

后台新增 `GET /api/admin/xingwei/business/after-sales/cases`，用于按具体售后流水查找 V4A 可处理记录。要求 `order:view`；计数和翻页均先按当前账号的 ALL、DEPARTMENT 或 SELF 数据范围过滤。

查询参数：

- `status`：`OPEN`、`IN_PROGRESS`、`RESOLVED`，省略时返回全部。无处理状态行的 V70 记录按 `OPEN` 返回。
- `category`：`DELIVERY`、`CANCELLATION`、`REFUND_REVIEW`、`OTHER`。
- `q`：业务编号、订单号或客户提交正文关键词，最多 80 字。
- `page`：从 0 开始，每页 50 条，最多第 200 页。

响应包含 `items`、`totalCount`、`page`、`pageSize`、`hasNext` 及实际筛选条件。每条记录提供 `businessNo` 和 V4A 的记录 ID、原文、处理状态及版本号；员工可直接使用 V4A 订单级接口推进处理。排序按客户提交时间和记录 ID 倒序；未授权订单不会进入计数或结果。

此队列覆盖 V70 独立流水。更早仅存放于订单备注的历史售后说明仍可在原订单级列表查看，但没有可操作的记录 ID，不会伪装为待处理工单。V4B 不新增数据库迁移、不触发退款或支付，也不改变客户可见内容。
