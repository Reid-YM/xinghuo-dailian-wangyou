-- =============================================================================
-- V56 账号 · 询价联系方式
--
-- 为什么单独存一列，而不是复用 contact_type / contact_value：
-- 后者是**登录凭据**（uk_customer_contact_norm 唯一索引 + 登录按它匹配账号）。
-- 若允许在后台「免验证」改它，等于允许任何拿到会话的人把账号改绑成自己的
-- 手机号/微信号，属于账号被顶走的路径。
--
-- 这里存的是「客服回访用」的联系方式，与身份验证解耦：前台询价时已登录用户
-- 若已绑定，就无需再填一遍；未绑定时仍按原流程填写。
-- 按需求暂不做验证，因此不要用它做任何身份判定。
-- =============================================================================

ALTER TABLE customer_accounts
    ADD COLUMN quote_contact_type VARCHAR(16) NULL COMMENT 'WECHAT / PHONE / QQ，仅用于客服回访',
    ADD COLUMN quote_contact_value VARCHAR(120) NULL COMMENT '原样保存，用于展示与客服核对',
    ADD COLUMN quote_contact_updated_at DATETIME(6) NULL;
