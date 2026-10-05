CREATE TABLE IF NOT EXISTS seller_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_account_id BIGINT NULL,
  display_name VARCHAR(80) NOT NULL,
  seller_level VARCHAR(24) NOT NULL DEFAULT 'NEW',
  commission_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1200,
  rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  rating_count INT NOT NULL DEFAULT 0,
  completed_orders INT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_seller_customer_account (customer_account_id),
  INDEX idx_seller_level_status (seller_level, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS booster_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_account_id BIGINT NULL,
  display_name VARCHAR(80) NOT NULL,
  skill_tags VARCHAR(500) NULL,
  capacity_status VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE',
  level_code VARCHAR(24) NOT NULL DEFAULT 'NEW',
  rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  completed_tasks INT NOT NULL DEFAULT 0,
  status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_booster_customer_account (customer_account_id),
  INDEX idx_booster_capacity_status (capacity_status, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS service_products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  service_id BIGINT NULL,
  game_code VARCHAR(40) NOT NULL,
  category_code VARCHAR(40) NOT NULL,
  title VARCHAR(120) NOT NULL,
  slug VARCHAR(160) NOT NULL,
  description VARCHAR(1000) NULL,
  seo_title VARCHAR(160) NULL,
  seo_description VARCHAR(260) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  sort_order INT NOT NULL DEFAULT 100,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_service_products_slug (slug),
  INDEX idx_products_game_category (game_code, category_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_offers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  seller_id BIGINT NULL,
  price_mode VARCHAR(24) NOT NULL DEFAULT 'QUOTE',
  price_fixed DECIMAL(10,2) NULL,
  price_min DECIMAL(10,2) NULL,
  price_max DECIMAL(10,2) NULL,
  delivery_time_text VARCHAR(80) NULL,
  stock_status VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE',
  commission_rate DECIMAL(5,4) NULL,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_offers_product_status (product_id, status),
  INDEX idx_offers_seller_status (seller_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS escrow_cases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_no VARCHAR(32) NOT NULL,
  buyer_account_id BIGINT NULL,
  seller_id BIGINT NULL,
  order_id CHAR(36) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
  amount DECIMAL(10,2) NULL,
  reason VARCHAR(500) NULL,
  resolution VARCHAR(1000) NULL,
  opened_at DATETIME(6) NOT NULL,
  resolved_at DATETIME(6) NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_escrow_business_no (business_no),
  INDEX idx_escrow_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS booster_assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id CHAR(36) NOT NULL,
  booster_id BIGINT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ASSIGNED',
  assigned_at DATETIME(6) NOT NULL,
  accepted_at DATETIME(6) NULL,
  completed_at DATETIME(6) NULL,
  note VARCHAR(1000) NULL,
  INDEX idx_booster_assignment_order (order_id),
  INDEX idx_booster_assignment_booster (booster_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS seo_landing_pages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  game_code VARCHAR(40) NOT NULL,
  category_code VARCHAR(40) NOT NULL,
  service_id BIGINT NULL,
  slug VARCHAR(180) NOT NULL,
  title VARCHAR(160) NOT NULL,
  description VARCHAR(500) NULL,
  keyword_group VARCHAR(260) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_seo_landing_slug (slug),
  INDEX idx_seo_game_category (game_code, category_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
