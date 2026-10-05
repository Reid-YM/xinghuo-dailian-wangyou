ALTER TABLE customer_accounts
    ADD COLUMN public_user_id VARCHAR(24) NULL AFTER contact_verified;

UPDATE customer_accounts
   SET public_user_id = CONCAT('XH', LPAD(id, 8, '0'))
 WHERE public_user_id IS NULL OR public_user_id = '';

ALTER TABLE customer_accounts
    MODIFY public_user_id VARCHAR(24) NOT NULL,
    ADD UNIQUE KEY uk_customer_public_user_id (public_user_id);
