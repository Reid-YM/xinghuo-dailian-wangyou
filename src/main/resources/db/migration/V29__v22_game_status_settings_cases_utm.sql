-- V22 P0-01/P0-02/P1-02/P1-07
-- 1) 每个游戏独立的运营状态（单一来源，支持多游戏）
-- 2) site_settings 公开配置单一来源
-- 3) 真实公开案例开关与脱敏字段
-- 4) 询价来源 UTM 记录
-- 全部为向后兼容新增，不删除旧字段。

SET @now = UTC_TIMESTAMP(6);

-- ---------- 1. 游戏运营状态 ----------
CREATE TABLE IF NOT EXISTS game_status (
    game_code      VARCHAR(32)  NOT NULL,
    status_code    VARCHAR(16)  NOT NULL,
    public_label   VARCHAR(80)  NOT NULL,
    public_detail  VARCHAR(240) NULL,
    cta_label      VARCHAR(40)  NULL,
    effective_at   DATETIME(6)  NULL,
    updated_by     VARCHAR(80)  NULL,
    updated_at     DATETIME(6)  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (game_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 从现有 site_config 单一状态迁移出 MW4 的游戏级状态
INSERT INTO game_status (game_code, status_code, public_label, public_detail, cta_label, effective_at, updated_by, updated_at)
SELECT 'CODMW4',
       CASE service_status
            WHEN 'OPEN' THEN 'OPEN'
            WHEN 'BUSY' THEN 'LIMITED'
            WHEN 'CLOSED' THEN 'PAUSED'
            WHEN 'PREHEAT' THEN 'PREORDER'
            WHEN 'SUSPENDED' THEN 'PAUSED'
            WHEN 'MAINTENANCE' THEN 'MAINTENANCE'
            ELSE 'PREORDER' END,
       COALESCE(NULLIF(status_short_text, ''), 'MW4 正式版预约中'),
       '上线后陆续开放服务，开放前提交为提前询价',
       COALESCE(NULLIF(primary_button_label, ''), '提前询价'),
       @now, 'system-migration', @now
FROM site_config WHERE id = 1
ON DUPLICATE KEY UPDATE updated_at = @now;

-- 战狗为长期在营项目，默认可接单
INSERT INTO game_status (game_code, status_code, public_label, public_detail, cta_label, effective_at, updated_by, updated_at)
VALUES ('WAR_DOGS', 'OPEN', '战狗 正常接单', '长期在营，可按进度提交需求', '立即询价', @now, 'system-migration', @now)
ON DUPLICATE KEY UPDATE updated_at = @now;

-- ---------- 2. site_settings 公开配置单一来源 ----------
CREATE TABLE IF NOT EXISTS site_settings (
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(500) NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO site_settings (setting_key, setting_value, updated_at)
SELECT 'legalEntityName', legal_entity_name, @now FROM site_config WHERE id = 1
ON DUPLICATE KEY UPDATE updated_at = @now;
INSERT INTO site_settings (setting_key, setting_value, updated_at)
SELECT 'brandName', brand_name, @now FROM site_config WHERE id = 1
ON DUPLICATE KEY UPDATE updated_at = @now;
INSERT INTO site_settings (setting_key, setting_value, updated_at)
SELECT 'wechat', wechat_id, @now FROM site_config WHERE id = 1
ON DUPLICATE KEY UPDATE updated_at = @now;
INSERT INTO site_settings (setting_key, setting_value, updated_at)
SELECT 'serviceHours', service_hours, @now FROM site_config WHERE id = 1
ON DUPLICATE KEY UPDATE updated_at = @now;
INSERT INTO site_settings (setting_key, setting_value, updated_at)
VALUES ('icpNumber', '蜀ICP备2026047129号', @now),
       ('publicSecurityNumber', '川公网安备51162102000262号', @now),
       ('siteTitleSuffix', '星火网游代练平台', @now),
       ('defaultOgImage', '/og.png', @now),
       ('faviconPath', '/favicon.svg', @now)
ON DUPLICATE KEY UPDATE updated_at = @now;

-- ---------- 3. 公开案例 ----------
ALTER TABLE customer_orders
    ADD COLUMN allow_public_case TINYINT(1) NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN public_summary   VARCHAR(240) NULL AFTER allow_public_case,
    ADD COLUMN finished_at      DATETIME(6)  NULL AFTER public_summary,
    ADD COLUMN public_enabled_at DATETIME(6) NULL AFTER finished_at;

-- ---------- 4. 询价来源 UTM ----------
ALTER TABLE quotes
    ADD COLUMN first_touch_source VARCHAR(64)  NULL AFTER customer_ip_hash,
    ADD COLUMN utm_source         VARCHAR(64)  NULL AFTER first_touch_source,
    ADD COLUMN utm_medium         VARCHAR(64)  NULL AFTER utm_source,
    ADD COLUMN utm_campaign       VARCHAR(64)  NULL AFTER utm_medium,
    ADD COLUMN referrer           VARCHAR(300) NULL AFTER utm_campaign;
