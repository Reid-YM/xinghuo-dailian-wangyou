-- Full DMZ catalog based on https://upblazed.com/dmz
-- Original DMZ (MW2) and MW4 DMZ services are both included under the existing CODMW4 DMZ category.
-- Every DMZ service is fixed at CNY 20.00 and uses a distinct cover image.

UPDATE services SET name = '原版 DMZ 任务完成', short_desc = '原版 DMZ 阵营任务、合约与任务链推进，可按单任务或整阶完成', price_mode = 'FIXED', price_fixed = 20.00, price_min = NULL, price_max = NULL, duration_text = '按任务进度', status = 'AVAILABLE', cover_image = '/assets/covers/guides/mw4-dmz-store.jpg', hot_score = 88.00, sort_order = 10, is_active = 1, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-missions';
UPDATE services SET name = '原版 DMZ 武器箱撤离', short_desc = '武器箱定位、护送与安全撤离，失败按约定补打', price_mode = 'FIXED', price_fixed = 20.00, price_min = NULL, price_max = NULL, duration_text = '按任务进度', status = 'AVAILABLE', cover_image = '/assets/covers/guides/mw4-dmz-tune-in.jpg', hot_score = 89.00, sort_order = 22, is_active = 1, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz';
UPDATE services SET name = 'MW4 DMZ FOB 进度', short_desc = 'FOB 设施解锁与升级，按站点或整套基地推进', price_mode = 'FIXED', price_fixed = 20.00, price_min = NULL, price_max = NULL, duration_text = '按 FOB 目标确认', status = 'AVAILABLE', cover_image = '/assets/covers/service/codmw4-5.jpg', hot_score = 88.00, sort_order = 31, is_active = 1, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-progression';
UPDATE services SET name = 'MW4 DMZ 武器解锁 / 武器手册', short_desc = '武器手册获取、现金准备与武器商解锁推进', price_mode = 'FIXED', price_fixed = 20.00, price_min = NULL, price_max = NULL, duration_text = '按目标武器确认', status = 'AVAILABLE', cover_image = '/assets/covers/service/codmw4-1.jpg', hot_score = 89.00, sort_order = 40, is_active = 1, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-weapon-unlock';

INSERT INTO services (slug, game_code, name, category, short_desc, price_mode, price_fixed, price_min, price_max,
                      duration_text, status, cover_image, hot_score, manual_priority, published_at,
                      is_active, sort_order, created_at, updated_at, version)
VALUES
('dmz-key-farming', 'CODMW4', '原版 DMZ 钥匙刷取', 'DMZ',
 '指定钥匙或批量钥匙刷取，含据点与特殊钥匙目标',
 'FIXED', 20.00, NULL, NULL, '按钥匙数量确认', 'AVAILABLE',
 '/assets/covers/guides/mw4-dmz-interior.jpg', 87.00, 0, NOW(), 1, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('dmz-loadout-recovery', 'CODMW4', '原版 DMZ 装备找回', 'DMZ',
 '保险武器与装备找回，缩短冷却并重建可用配装',
 'FIXED', 20.00, NULL, NULL, '约 1 天', 'AVAILABLE',
 '/assets/covers/service/codmw4-6.jpg', 85.00, 0, NOW(), 1, 21, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('dmz-faction-rep', 'CODMW4', '原版 DMZ 阵营声望', 'DMZ',
 '按指定阵营与目标等级推进声望，解锁保险栏位与奖励',
 'FIXED', 20.00, NULL, NULL, '按阵营等级确认', 'AVAILABLE',
 '/assets/covers/service/codmw4-4.jpg', 86.00, 0, NOW(), 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-bounty', 'CODMW4', 'MW4 DMZ 悬赏与通缉度', 'DMZ',
 '悬赏目标猎杀、狗牌带出与通缉度推进',
 'FIXED', 20.00, NULL, NULL, '按合约数量确认', 'AVAILABLE',
 '/assets/covers/guides/mw4-forest-squad.jpg', 90.00, 0, NOW(), 1, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-lieutenants', 'CODMW4', 'MW4 DMZ 中尉首领猎杀', 'DMZ',
 '中尉首领情报、击杀与首领武器目标推进',
 'FIXED', 20.00, NULL, NULL, '按首领目标确认', 'AVAILABLE',
 '/assets/covers/service/codmw4-3.jpg', 88.00, 0, NOW(), 1, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-mia-rescue', 'CODMW4', 'MW4 DMZ MIA 干员救援', 'DMZ',
 'MIA 干员救援与救援现金推进，保留干员进度',
 'FIXED', 20.00, NULL, NULL, '按救援次数确认', 'AVAILABLE',
 '/assets/covers/guides/mw4-korea-village.png', 87.00, 0, NOW(), 1, 13, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-story-missions', 'CODMW4', 'MW4 DMZ 剧情任务', 'DMZ',
 '剧情任务、任务指令与剧情进度推进，支持跟车或上号',
 'FIXED', 20.00, NULL, NULL, '按任务进度确认', 'AVAILABLE',
 '/assets/covers/market/codmw4.jpg', 89.00, 0, NOW(), 1, 14, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-cash', 'CODMW4', 'MW4 DMZ 现金刷取', 'DMZ',
 'FOB 钱包现金刷取，未经确认不消费账户现金',
 'FIXED', 20.00, NULL, NULL, '按目标现金确认', 'AVAILABLE',
 '/assets/covers/service/codmw4-2.jpg', 86.00, 0, NOW(), 1, 23, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-crafting', 'CODMW4', 'MW4 DMZ 制作与配方', 'DMZ',
 '材料与配方刷取，按需制作背包、护甲与连杀奖励',
 'FIXED', 20.00, NULL, NULL, '按配方目标确认', 'AVAILABLE',
 '/assets/covers/game/codmw4.jpg', 85.00, 0, NOW(), 1, 24, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),

('mw4-dmz-leveling', 'CODMW4', 'MW4 DMZ 干员等级 / 特性 / 仓库', 'DMZ',
 '干员等级、DMZ 经验、特性点与仓库容量推进',
 'FIXED', 20.00, NULL, NULL, '按等级目标确认', 'AVAILABLE',
 '/assets/covers/guides/mw4-dmz-store-crop.jpg', 87.00, 0, NOW(), 1, 32, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)

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
