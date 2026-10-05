-- MW4 DMZ service catalog, based on the UPBLAZED DMZ categories.
-- All DMZ products are fixed at CNY 20.00 and use distinct promotional covers.

UPDATE services
SET name = 'DMZ 撤离 / 高价值物资',
    short_desc = 'DMZ 撤离、钥匙与高价值物资目标推进',
    price_mode = 'FIXED',
    price_fixed = 20.00,
    price_min = NULL,
    price_max = NULL,
    duration_text = '约 1 天',
    status = 'AVAILABLE',
    cover_image = '/assets/covers/guides/mw4-dmz-interior.jpg',
    is_active = 1,
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE slug = 'mw4-dmz';

INSERT INTO services (slug, game_code, name, category, short_desc, price_mode, price_fixed, price_min, price_max,
                      duration_text, status, cover_image, hot_score, manual_priority, published_at,
                      is_active, sort_order, created_at, updated_at, version)
VALUES
('mw4-dmz-missions', 'CODMW4', 'DMZ 任务 / 合约', 'DMZ',
 '阵营、剧情、悬赏、MIA 救援与中尉首领目标推进',
 'FIXED', 20.00, NULL, NULL, '按任务进度', 'AVAILABLE',
 '/assets/covers/guides/mw4-dmz-store.jpg', 88.00, 0, NOW(), 1, 35, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-progression', 'CODMW4', 'DMZ 等级 / 声望', 'DMZ',
 'MW4 DMZ 等级、FOB、干员特质与仓库进度推进',
 'FIXED', 20.00, NULL, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/codmw4-5.jpg', 86.00, 0, NOW(), 1, 55, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-weapon-unlock', 'CODMW4', 'DMZ 武器解锁 / 武器手册', 'DMZ',
 'DMZ 武器解锁、武器手册与指定枪械目标推进',
 'FIXED', 20.00, NULL, NULL, '按目标枪械确认', 'AVAILABLE',
 '/assets/covers/guides/mw4-dmz-tune-in.jpg', 87.00, 0, NOW(), 1, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)

ON DUPLICATE KEY UPDATE
    game_code = VALUES(game_code),
    name = VALUES(name),
    category = VALUES(category),
    short_desc = VALUES(short_desc),
    price_mode = VALUES(price_mode),
    price_fixed = VALUES(price_fixed),
    price_min = VALUES(price_min),
    price_max = VALUES(price_max),
    duration_text = VALUES(duration_text),
    status = VALUES(status),
    cover_image = VALUES(cover_image),
    hot_score = VALUES(hot_score),
    manual_priority = VALUES(manual_priority),
    published_at = COALESCE(published_at, VALUES(published_at)),
    is_active = VALUES(is_active),
    sort_order = VALUES(sort_order),
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1;
