CREATE TABLE IF NOT EXISTS admin_users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS site_config (
  id BIGINT PRIMARY KEY,
  service_status VARCHAR(16) NOT NULL,
  hero_title VARCHAR(120) NOT NULL,
  hero_subtitle VARCHAR(240) NOT NULL,
  starting_price DECIMAL(10,2) NOT NULL,
  hero_image VARCHAR(500) NULL,
  trust_tags VARCHAR(500) NOT NULL,
  wechat_id VARCHAR(100) NULL,
  qq_id VARCHAR(100) NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS services (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slug VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(32) NOT NULL,
  short_desc VARCHAR(240) NOT NULL,
  price_mode VARCHAR(16) NOT NULL,
  price_fixed DECIMAL(10,2) NULL,
  price_min DECIMAL(10,2) NULL,
  price_max DECIMAL(10,2) NULL,
  duration_text VARCHAR(100) NOT NULL,
  status VARCHAR(16) NOT NULL,
  cover_image VARCHAR(500) NULL,
  hot_score DECIMAL(8,2) NOT NULL DEFAULT 0,
  manual_priority INT NOT NULL DEFAULT 0,
  source_event_id BIGINT NULL,
  published_at DATETIME(6) NULL,
  expires_at DATETIME(6) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_services_public (is_active, status, sort_order),
  INDEX idx_services_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS upload_assets (
  id CHAR(36) PRIMARY KEY,
  kind VARCHAR(32) NOT NULL,
  stored_name VARCHAR(255) NOT NULL UNIQUE,
  original_name VARCHAR(255) NULL,
  content_type VARCHAR(64) NOT NULL,
  byte_size BIGINT NOT NULL,
  owner_type VARCHAR(32) NULL,
  owner_ref VARCHAR(100) NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_upload_owner (owner_type, owner_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quotes (
  id CHAR(36) PRIMARY KEY,
  game VARCHAR(100) NOT NULL,
  demand VARCHAR(200) NOT NULL,
  current_progress VARCHAR(500) NULL,
  screenshot_asset_id CHAR(36) NULL,
  platform VARCHAR(50) NULL,
  expected_time VARCHAR(100) NULL,
  status VARCHAR(24) NOT NULL,
  idempotency_key VARCHAR(100) NOT NULL UNIQUE,
  customer_ip_hash VARCHAR(128) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_quotes_status_created (status, created_at),
  CONSTRAINT fk_quote_upload FOREIGN KEY (screenshot_asset_id) REFERENCES upload_assets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_orders (
  id CHAR(36) PRIMARY KEY,
  order_no VARCHAR(40) NOT NULL UNIQUE,
  status VARCHAR(32) NOT NULL,
  progress_percent INT NULL,
  progress_text VARCHAR(240) NOT NULL,
  progress_image_asset_id CHAR(36) NULL,
  customer_note VARCHAR(500) NULL,
  internal_note VARCHAR(1200) NULL,
  updated_at DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  INDEX idx_orders_status_updated (status, updated_at),
  CONSTRAINT fk_order_upload FOREIGN KEY (progress_image_asset_id) REFERENCES upload_assets(id) ON DELETE SET NULL,
  CONSTRAINT chk_progress_percent CHECK (progress_percent IS NULL OR (progress_percent >= 0 AND progress_percent <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hot_slots (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  slot_no INT NOT NULL UNIQUE,
  service_id BIGINT NULL,
  mode VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_slot_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE SET NULL,
  CONSTRAINT chk_slot_no CHECK (slot_no BETWEEN 1 AND 2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id VARCHAR(100) NOT NULL,
  operator_name VARCHAR(100) NOT NULL,
  before_json TEXT NULL,
  after_json TEXT NULL,
  reason VARCHAR(500) NULL,
  ip_address VARCHAR(64) NULL,
  request_id VARCHAR(100) NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_audit_target (target_type, target_id),
  INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tracking_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_name VARCHAR(64) NOT NULL,
  path VARCHAR(255) NULL,
  ref_value VARCHAR(255) NULL,
  ip_hash VARCHAR(128) NULL,
  created_at DATETIME(6) NOT NULL,
  INDEX idx_tracking_event_created (event_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_config (id, service_status, hero_title, hero_subtitle, starting_price, trust_tags, updated_at, version)
VALUES (1, 'OPEN', 'COD 系列任务代练', '武器升级·迷彩·活动任务·排位', 9.90, '纯绿手打|进度可查|异常及时沟通', UTC_TIMESTAMP(6), 0)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO hot_slots (slot_no, service_id, mode, updated_at)
VALUES (1, NULL, 'AUTO', UTC_TIMESTAMP(6)), (2, NULL, 'AUTO', UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE slot_no=VALUES(slot_no);


INSERT INTO services (slug,name,category,short_desc,price_mode,price_fixed,price_min,price_max,duration_text,status,hot_score,manual_priority,is_active,sort_order,published_at,created_at,updated_at,version)
VALUES
('weapon-leveling','武器升级','WEAPON','按目标等级与当前进度报价','RANGE',NULL,20.00,60.00,'约30–90分钟','AVAILABLE',90.00,10,TRUE,10,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('camo-task','迷彩任务','CAMO','按迷彩目标与剩余条件确认','QUOTE',NULL,NULL,NULL,'按进度确认','AVAILABLE',80.00,5,TRUE,20,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('event-task','当前活动','ACTIVITY','限时活动任务，按任务内容确认','QUOTE',NULL,NULL,NULL,'按活动剩余时间确认','AVAILABLE',95.00,20,TRUE,5,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('other-task','其他任务','OTHER','不知道名称可直接发截图判断','QUOTE',NULL,NULL,NULL,'人工确认','AVAILABLE',30.00,0,TRUE,99,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)
ON DUPLICATE KEY UPDATE slug=VALUES(slug);
