# V3Y 后台售后记录接口

状态：后端 PR #437 与版本 PR #438 已通过 Java/Maven、Chromium/WebKit CI 并合入 `main`；主线 CI #38578260053 双绿。发布请求 `045971f` 经 `deploy/production` 的 `.deploy-request` 门禁，将主线 `7239e0f` 发布到生产；18080 候选、8081 金丝雀、8080 正式入口与发布后核验全部通过（Deploy Production #38578842092）。本状态 PR 将 V3Y 标记为已上线。

`GET /api/admin/xingwei/business/orders/after-sales` 保留原有权限、数据范围、搜索、分类和分页逻辑。每个可见订单现在增加 `afterSalesRecords` 数组，记录包含 `category`（中文问题类型）、`submittedAt`（ISO 时间）和 `content`（纯文本说明）。原有 `customerNote` 等字段继续返回，未发现规范售后记录时数组为空。

后台列表与客户订单详情使用同一解析器：仅识别已知问题类型和有效时间的系统售后块；普通备注及用户补充不计入记录。列表的 SQL 范围过滤仍在取数前执行，结构化解析只作用于当前页可见订单。

此阶段不改数据库、权限、前端、支付或企业微信配置。历史备注中的伪造系统头无法仅凭文本证明来源，因此结构化数组表示规范格式的历史记录，不代表已核实的客服处理状态，也不触发退款。
