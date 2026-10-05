-- MW4 camo services based on https://upblazed.com/mw4/camo-boost
-- Product images are local copies of the UPBLAZED product images.

UPDATE services
SET name = 'MW4 全迷彩代练',
    short_desc = '多人、战区和 DMZ 精通迷彩整枪进度推进',
    price_mode = 'QUOTE',
    price_fixed = NULL,
    price_min = 80.00,
    price_max = NULL,
    duration_text = '按剩余进度确认',
    status = 'AVAILABLE',
    cover_image = '/assets/covers/service/camo/mw4-camo-boost.webp',
    hot_score = 95.00,
    sort_order = 100,
    is_active = 1,
    updated_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE slug = 'camo-task';

INSERT INTO services (slug, game_code, name, category, short_desc, price_mode, price_fixed, price_min, price_max,
                      duration_text, status, cover_image, hot_score, manual_priority, published_at,
                      is_active, sort_order, created_at, updated_at, version)
VALUES
('mw4-tier-camo', 'CODMW4', 'MW4 Tier 迷彩', 'CAMO',
 '精通迷彩前置层级，按武器当前进度补差',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-camo-boost.webp', 88.00, 0, NOW(), 1, 110, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-mercurial-drift-camo', 'CODMW4', 'MW4 Mercurial Drift 迷彩', 'CAMO',
 '多人模式精通迷彩，指定武器推进',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-mercurial-drift.webp', 94.00, 0, NOW(), 1, 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-polyatomic-reforged-camo', 'CODMW4', 'MW4 Polyatomic Reforged 迷彩', 'CAMO',
 'MW2 经典迷彩重制版，多人模式精通目标',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-polyatomic-reforged.webp', 92.00, 0, NOW(), 1, 130, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-orion-reforged-camo', 'CODMW4', 'MW4 Orion Reforged 迷彩', 'CAMO',
 'MW2 经典迷彩重制版，多人模式精通目标',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-orion-reforged.webp', 93.00, 0, NOW(), 1, 140, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-parallax-camo', 'CODMW4', 'MW4 Parallax 迷彩', 'CAMO',
 '战区全新精通迷彩，指定武器推进',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-parallax.webp', 91.00, 0, NOW(), 1, 150, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-damascus-reforged-camo', 'CODMW4', 'MW4 Damascus Reforged 迷彩', 'CAMO',
 'MW 2019 经典迷彩重制版，战区精通目标',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-damascus-reforged.webp', 93.00, 0, NOW(), 1, 160, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-empyros-camo', 'CODMW4', 'MW4 Empyros 迷彩', 'CAMO',
 '战区全新精通迷彩，指定武器推进',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-empyros.webp', 90.00, 0, NOW(), 1, 170, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-chiral-camo', 'CODMW4', 'MW4 Chiral 迷彩', 'CAMO',
 'DMZ 精通迷彩，指定武器推进',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-chiral.webp', 91.00, 0, NOW(), 1, 180, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-ripple-tide-camo', 'CODMW4', 'MW4 Ripple Tide 迷彩', 'CAMO',
 'DMZ 精通迷彩，指定武器推进',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-ripple-tide.webp', 90.00, 0, NOW(), 1, 190, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-helio-camo', 'CODMW4', 'MW4 Helio 迷彩', 'CAMO',
 'DMZ 精通迷彩，按剩余挑战推进',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-helio.webp', 90.00, 0, NOW(), 1, 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-pyroclast-camo', 'CODMW4', 'MW4 Pyroclast 迷彩', 'CAMO',
 'Pyroclast 声望迷彩第一阶，武器满级后推进',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-pyroclast.webp', 89.00, 0, NOW(), 1, 210, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-pyroclast-verge-camo', 'CODMW4', 'MW4 Pyroclast Verge 迷彩', 'CAMO',
 'Pyroclast 声望迷彩第二阶段，含前置阶段',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-pyroclast-verge.webp', 89.00, 0, NOW(), 1, 220, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-pyroclast-scatter-camo', 'CODMW4', 'MW4 Pyroclast Scatter 迷彩', 'CAMO',
 'Pyroclast 声望迷彩第三阶段，含前置阶段',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-pyroclast-scatter.webp', 89.00, 0, NOW(), 1, 230, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-pyroclast-cascade-camo', 'CODMW4', 'MW4 Pyroclast Cascade 迷彩', 'CAMO',
 'Pyroclast 声望迷彩顶级阶段，含全部前置阶段',
 'QUOTE', NULL, 80.00, NULL, '按进度确认', 'AVAILABLE',
 '/assets/covers/service/camo/mw4-pyroclast-cascade.webp', 90.00, 0, NOW(), 1, 240, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)

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
