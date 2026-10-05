CREATE TABLE IF NOT EXISTS customer_accounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  display_name VARCHAR(80) NOT NULL,
  contact_method VARCHAR(40) NOT NULL,
  contact_value VARCHAR(120) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(24) NOT NULL DEFAULT 'BUYER',
  level_code VARCHAR(24) NOT NULL DEFAULT 'NEW',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  verified_at DATETIME(6) NULL,
  last_login_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_customer_contact (contact_method, contact_value),
  INDEX idx_customer_role_level (role, level_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE quotes ADD COLUMN customer_account_id BIGINT NULL;
ALTER TABLE quotes ADD INDEX idx_quotes_customer_account (customer_account_id, created_at);

ALTER TABLE customer_orders ADD COLUMN customer_account_id BIGINT NULL;
ALTER TABLE customer_orders ADD INDEX idx_orders_customer_account (customer_account_id, updated_at);
