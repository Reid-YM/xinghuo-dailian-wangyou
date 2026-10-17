-- Guest quote -> staff offer -> guest acceptance/payment foundation.
-- Browser sessions and business ownership are deliberately separate so a short-lived one-time
-- link can authorize another browser without creating a second customer/payment identity.

CREATE TABLE guest_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_hash CHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  expires_at DATETIME(6) NOT NULL,
  last_seen_at DATETIME(6) NULL,
  claimed_account_id BIGINT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_guest_session_token_hash (token_hash),
  INDEX idx_guest_session_expiry (status, expires_at),
  INDEX idx_guest_session_claimed (claimed_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客浏览器会话；仅保存随机凭证哈希';

CREATE TABLE guest_business_access (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_id CHAR(36) NOT NULL,
  business_no VARCHAR(40) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  claimed_account_id BIGINT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_guest_business_no (business_no),
  UNIQUE KEY uk_guest_quote_id (quote_id),
  CONSTRAINT fk_guest_access_quote FOREIGN KEY (quote_id)
    REFERENCES quotes(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客业务主体；订单与支付引用稳定 id';

CREATE TABLE guest_business_session_access (
  guest_session_id BIGINT NOT NULL,
  guest_access_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (guest_session_id, guest_access_id),
  INDEX idx_guest_business_sessions (guest_access_id, created_at),
  CONSTRAINT fk_guest_business_session FOREIGN KEY (guest_session_id)
    REFERENCES guest_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_guest_business_access FOREIGN KEY (guest_access_id)
    REFERENCES guest_business_access(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='允许同一游客业务安全授权给多个浏览器会话';

CREATE TABLE guest_access_link (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guest_access_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6) NULL,
  created_by VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_guest_access_link_token (token_hash),
  INDEX idx_guest_access_link_expiry (guest_access_id, used_at, expires_at),
  CONSTRAINT fk_guest_access_link_business FOREIGN KEY (guest_access_id)
    REFERENCES guest_business_access(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服生成的一次性游客业务访问链接；数据库只存 token 哈希';

CREATE TABLE quote_offer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  quote_id CHAR(36) NOT NULL,
  business_no VARCHAR(40) NOT NULL,
  version INT NOT NULL,
  subject VARCHAR(127) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
  estimated_duration VARCHAR(120) NULL,
  customer_message VARCHAR(1000) NULL,
  status VARCHAR(16) NOT NULL,
  quoted_by VARCHAR(100) NOT NULL,
  quoted_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NULL,
  accepted_at DATETIME(6) NULL,
  cancelled_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_quote_offer_version (quote_id, version),
  INDEX idx_quote_offer_business_status (business_no, status, id),
  CONSTRAINT fk_quote_offer_quote FOREIGN KEY (quote_id)
    REFERENCES quotes(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服正式报价；保留每次改价历史';

ALTER TABLE customer_orders
  ADD COLUMN guest_access_id BIGINT NULL AFTER customer_account_id,
  ADD INDEX idx_orders_guest_access (guest_access_id, updated_at),
  ADD CONSTRAINT fk_order_guest_access FOREIGN KEY (guest_access_id)
    REFERENCES guest_business_access(id) ON DELETE SET NULL;

ALTER TABLE payment_order
  ADD COLUMN guest_access_id BIGINT NULL AFTER user_id,
  ADD INDEX idx_payment_guest_access (guest_access_id, created_at),
  ADD CONSTRAINT fk_payment_guest_access FOREIGN KEY (guest_access_id)
    REFERENCES guest_business_access(id) ON DELETE SET NULL;
