-- =============================================================================
-- V57 账号 · 询价联系方式改为可存多条
--
-- 背景：V56 只允许存一条（quote_contact_type / quote_contact_value）。
-- 需求改为「可以添加多个联系方式」，因此在 customer_accounts 上用一个 JSON
-- 数组列承载：形如 [{"type":"PHONE","value":"13800000000"}, ...]。
--
-- 为什么仍然放在 customer_accounts 而不是拆新表：
-- 一个账号最多绑定几条、且总是整体读写，没有按条查询或关联的需求，
-- 独立表只会多一层 JOIN 与一套仓储代码，收益不足。
--
-- 这些字段只用于客服回访，不参与登录与身份判定（登录凭据是
-- contact_type / contact_value）。
-- =============================================================================

ALTER TABLE customer_accounts
    ADD COLUMN quote_contacts TEXT NULL COMMENT 'JSON 数组：[{"type":"PHONE","value":"13800000000"}]，仅用于客服回访';

-- 把 V56 已绑定的那一条原样迁进新列（值里可能出现引号，做一次转义）
UPDATE customer_accounts
SET quote_contacts = CONCAT(
        '[{"type":"', quote_contact_type,
        '","value":"', REPLACE(quote_contact_value, '"', '\\"'), '"}]')
WHERE quote_contact_type IS NOT NULL
  AND quote_contact_value IS NOT NULL
  AND quote_contact_value <> '';

-- 旧列的数据已迁走，删除以免留下两份真相
ALTER TABLE customer_accounts
    DROP COLUMN quote_contact_type,
    DROP COLUMN quote_contact_value,
    DROP COLUMN quote_contact_updated_at;
