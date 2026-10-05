-- =============================================================================
-- V41 支付用途
--
-- 支付单原先只通过 business_no 关联代练订单，但本站还有另一类商品：会员档位。
-- 它没有 customer_orders 记录，价格固定、买完即开通权益。
--
-- 用 purpose 显式区分用途，而不是把 "PLAN:PRO" 塞进 business_no：
-- 后者会让「业务订单号」这个字段承担两种语义，将来对账与查询必然出错。
--
-- purpose_ref 存放该用途下的引用：会员档位存 plan_code，代练服务存 business_no。
-- =============================================================================

ALTER TABLE payment_order
    ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'SERVICE_ORDER'
        COMMENT 'MEMBERSHIP / SERVICE_ORDER / CUSTOM' AFTER subject,
    ADD COLUMN purpose_ref VARCHAR(64) NULL
        COMMENT '用途引用：会员存 plan_code，服务存 business_no' AFTER purpose;

CREATE INDEX idx_payment_purpose ON payment_order (purpose, purpose_ref);
