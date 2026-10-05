-- Separate Warzone and DMZ camo products into their own first-level games.
-- Both games get a visible 迷彩任务 category.

INSERT INTO service_categories (game_code, code, name, icon_key, description, is_active, sort_order, created_at, updated_at)
VALUES ('DMZ', 'DMZ_CAMO', '迷彩任务', 'palette', 'DMZ 迷彩与挑战进度', 1, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), icon_key = VALUES(icon_key), description = VALUES(description),
    is_active = VALUES(is_active), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

UPDATE service_categories
SET name = '迷彩任务',
    icon_key = 'palette',
    description = '战区迷彩与挑战进度',
    is_active = 1,
    sort_order = 25,
    updated_at = CURRENT_TIMESTAMP
WHERE game_code = 'COD_WARZONE' AND code = 'WZ_CAMO';

UPDATE services SET game_code = 'COD_WARZONE', category = 'WZ_CAMO', sort_order = 25, manual_priority = 32, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-parallax-camo';
UPDATE services SET game_code = 'COD_WARZONE', category = 'WZ_CAMO', sort_order = 26, manual_priority = 31, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-damascus-reforged-camo';
UPDATE services SET game_code = 'COD_WARZONE', category = 'WZ_CAMO', sort_order = 27, manual_priority = 30, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-empyros-camo';

UPDATE services SET game_code = 'DMZ', category = 'DMZ_CAMO', sort_order = 10, manual_priority = 36, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-chiral-camo';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_CAMO', sort_order = 20, manual_priority = 35, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-ripple-tide-camo';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_CAMO', sort_order = 30, manual_priority = 34, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-helio-camo';
