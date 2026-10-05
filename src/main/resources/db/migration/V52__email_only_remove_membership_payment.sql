-- V52 邮箱账号唯一化，并下线会员/支付相关数据结构。
-- 部署前请确认已备份生产数据库；本迁移会删除支付、退款、会员权益和第三方身份绑定历史。

UPDATE customer_accounts
   SET contact_type = 'EMAIL',
       contact_value = COALESCE(email, contact_value),
       contact_norm = COALESCE(email_norm, contact_norm),
       contact_verified = email_verified
 WHERE email_norm IS NOT NULL;

DROP TABLE IF EXISTS payment_refund;
DROP TABLE IF EXISTS payment_transaction;
DROP TABLE IF EXISTS payment_webhook_event;
DROP TABLE IF EXISTS payment_order;
DROP TABLE IF EXISTS membership_grant;
DROP TABLE IF EXISTS membership_plan;
DROP TABLE IF EXISTS user_identity;
