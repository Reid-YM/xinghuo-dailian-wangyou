# V4C 售后处理闭环

V4C 将 V4B 售后队列接入独立处理工作台 `/admin/xingwei/after-sales/cases`。`order:view` 可按现有数据范围筛选、查看记录及事件历史；`order:manage` 才能推进状态。提交必须携带 `expectedVersion`，冲突返回 409 并要求重新读取。

`V72__customer_order_after_sales_case_events.sql` 增加事件表。每次成功状态迁移在同一事务内写入内部处理说明、可选客户回复、处理人和时间；原 V71 状态表仍是当前状态真值。V72 为增量建表，旧 8080 实例不依赖它，可用于零停机切换。既有 V4A 动作保留在审计日志，不伪造为 V72 事件。

客户订单详情新增 `afterSalesCases`，仅在订单归属校验通过后返回状态和最近一次明确填写的客户回复。内部处理说明及处理人不进入客户响应。`afterSalesRecords` 和旧备注历史继续保留；旧备注中没有独立流水 ID 的记录不会显示为可处理工单。客户页面以纯文本展示回复。

本阶段不发起退款、不改变支付或企业微信写入，也不启用真实收款。V3H 小程序微信支付 PR #384 和企业微信写入 PR #372 继续独立验收。发布已通过 PR #449、main 双 CI、`deploy/production` 的 `.deploy-request` 和 18080/8081/8080 门禁（Deploy Production #38601312604）。V4C 已标为稳定生产。后续版本仍遵守同一门禁。
