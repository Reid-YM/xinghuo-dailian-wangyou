-- V21：工具内容（迷彩助手 / 配枪中心）的更新时间与公开状态，改为后台可配置。
SET @now = UTC_TIMESTAMP(6);

ALTER TABLE site_config
    ADD COLUMN tool_updated_note   VARCHAR(120) NULL AFTER seo_description,
    ADD COLUMN tool_public_enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER tool_updated_note;

UPDATE site_config
SET tool_updated_note   = '工具内容更新：2026-09-27',
    tool_public_enabled = 1,
    updated_at          = @now
WHERE id = 1;
