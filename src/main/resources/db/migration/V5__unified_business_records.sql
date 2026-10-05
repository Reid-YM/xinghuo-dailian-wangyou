-- 二期重构增量：询价/订单统一业务编号 + 客户端业务记录
-- 新编号格式：XH + 10 位大写字母/数字，例如 XH7KM4Q2AZ8P。
-- 已有数据使用 UUID 前 10 位生成简短编号；询价转成的订单沿用同一个业务编号。

ALTER TABLE quotes
  ADD COLUMN business_no VARCHAR(20) NULL AFTER id;

UPDATE quotes
SET business_no = CONCAT('XH', UPPER(SUBSTRING(REPLACE(id, '-', ''), 1, 10)))
WHERE business_no IS NULL OR business_no = '';

ALTER TABLE quotes
  MODIFY business_no VARCHAR(20) NOT NULL,
  ADD UNIQUE INDEX uq_quotes_business_no (business_no);

ALTER TABLE customer_orders
  ADD COLUMN business_no VARCHAR(20) NULL AFTER order_no;

UPDATE customer_orders o
JOIN quotes q ON q.id = o.source_quote_id
SET o.business_no = q.business_no
WHERE o.business_no IS NULL OR o.business_no = '';

UPDATE customer_orders
SET business_no = CONCAT('XH', UPPER(SUBSTRING(REPLACE(id, '-', ''), 1, 10)))
WHERE business_no IS NULL OR business_no = '';

ALTER TABLE customer_orders
  MODIFY business_no VARCHAR(20) NOT NULL,
  ADD UNIQUE INDEX uq_orders_business_no (business_no);

-- 只替换系统旧默认文案，不覆盖用户已经自行修改的内容。
UPDATE site_config
SET contact_intro = '提交需求后发送业务编号即可一对一沟通；询价转订单后编号保持不变。'
WHERE id = 1 AND contact_intro = '询价后建议发送询价编号；已有订单请直接发送订单号。';

UPDATE site_config
SET quote_success_text = '提交成功，业务编号：{id}。可在“我的记录”查看进度，或联系客服一对一咨询。'
WHERE id = 1 AND quote_success_text = '提交成功，询价编号：{id}。请联系客服确认最终报价。';

UPDATE site_config
SET order_help_text = '输入统一业务编号即可查询询价或订单进度。'
WHERE id = 1 AND order_help_text = '输入客服提供的订单号，查看当前状态、进度和最近更新。';
