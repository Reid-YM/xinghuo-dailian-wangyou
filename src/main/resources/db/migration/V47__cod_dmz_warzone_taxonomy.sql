-- Service taxonomy optimization: promote DMZ and Warzone to first-level games.
-- DMZ services move out of CODMW4 into the dedicated DMZ game.

INSERT INTO game_catalog (code, name, name_en, aliases, icon_key, description, publisher, genres, logo_image, cover_image,
                          popularity_score, is_featured, business_status, seo_slug, seo_title, seo_description,
                          is_active, sort_order, created_at, updated_at)
VALUES ('DMZ', 'DMZ', 'Call of Duty: DMZ',
        'DMZ,使命召唤DMZ,COD DMZ,MW2 DMZ,MW4 DMZ',
        'gamepad', 'DMZ 撤离、任务、物资、等级与武器解锁服务', 'Activision', 'FPS,撤离射击,任务推进',
        NULL, '/assets/covers/guides/mw4-dmz-store.jpg', 96, 1, 'ACTIVE', 'dmz',
        'DMZ 任务代练服务 | 星火网游代练平台',
        'DMZ 任务、撤离、物资、等级与武器解锁服务。按当前进度报价，提交需求拿业务编号，客服一对一确认。网页不收账号密码。',
        1, 11, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), name_en = VALUES(name_en), aliases = VALUES(aliases), icon_key = VALUES(icon_key),
    description = VALUES(description), publisher = VALUES(publisher), genres = VALUES(genres),
    cover_image = VALUES(cover_image), popularity_score = VALUES(popularity_score), is_featured = VALUES(is_featured),
    business_status = VALUES(business_status), seo_slug = VALUES(seo_slug), seo_title = VALUES(seo_title),
    seo_description = VALUES(seo_description), is_active = VALUES(is_active), sort_order = VALUES(sort_order),
    updated_at = CURRENT_TIMESTAMP;

UPDATE game_catalog
SET name = '战区',
    name_en = 'Call of Duty: Warzone',
    aliases = '战区,Warzone,COD Warzone,战争地带',
    icon_key = 'gamepad',
    description = 'Call of Duty 战区排位、迷彩、武器、账号等级与赛季内容',
    publisher = 'Activision',
    genres = 'FPS,大逃杀,多人竞技',
    cover_image = '/assets/covers/market/cod_warzone.webp',
    popularity_score = 95,
    is_featured = 0,
    business_status = 'ACTIVE',
    seo_slug = 'warzone',
    seo_title = '战区任务服务 | 星火网游代练平台',
    seo_description = '战区排位、迷彩、武器升级、账号等级与赛季内容服务。按目标报价，提交需求拿业务编号，客服一对一确认。网页不收账号密码。',
    is_active = 1,
    sort_order = 12,
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'COD_WARZONE';

UPDATE game_catalog SET sort_order = 13, updated_at = CURRENT_TIMESTAMP WHERE code = 'WAR_DOGS';

INSERT INTO service_categories (game_code, code, name, icon_key, description, is_active, sort_order, created_at, updated_at)
VALUES
('DMZ', 'DMZ_MISSIONS', 'DMZ任务', 'crosshair', '任务、剧情、悬赏、首领与 MIA 救援', 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DMZ', 'DMZ_EXTRACTION', '撤离与物资', 'layers', '撤离、钥匙、装备找回、武器箱、现金与制作', 1, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DMZ', 'DMZ_PROGRESSION', '等级与进度', 'rank', '阵营声望、FOB、干员等级、特性与仓库', 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DMZ', 'DMZ_WEAPON', '武器解锁', 'weapon', '武器手册、武器商与指定武器目标', 1, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COD_WARZONE', 'WZ_CAMO', '迷彩解锁', 'palette', '战区迷彩与挑战进度', 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COD_WARZONE', 'WZ_ACCOUNT', '账号等级', 'trophy', '账号等级、声望与经验推进', 1, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COD_WARZONE', 'WZ_CHALLENGES', '挑战任务', 'calendar', '每日、周常与赛季挑战', 1, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COD_WARZONE', 'WZ_NUKE', '核弹目标', 'star', '战区核弹与高难目标', 1, 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COD_WARZONE', 'WZ_BOT', '机器人局', 'crosshair', '机器人局刷取与训练目标', 1, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('COD_WARZONE', 'WZ_COACHING', '陪练 / 教学', 'service', '一对一陪练与战术教学', 1, 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), icon_key = VALUES(icon_key), description = VALUES(description),
    is_active = VALUES(is_active), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

UPDATE service_categories SET name = '排位竞技', icon_key = 'rank', description = '战区排位与段位推进', sort_order = 10, is_active = 1, updated_at = CURRENT_TIMESTAMP WHERE game_code = 'COD_WARZONE' AND code = 'RANKED';
UPDATE service_categories SET name = '武器 / 配装解锁', icon_key = 'weapon', description = '武器等级、配装与配件解锁', sort_order = 20, is_active = 1, updated_at = CURRENT_TIMESTAMP WHERE game_code = 'COD_WARZONE' AND code = 'LOADOUT';
UPDATE service_categories SET name = '战斗通行证 / 活动', icon_key = 'event', description = '战斗通行证、限时活动与赛季任务', sort_order = 40, is_active = 1, updated_at = CURRENT_TIMESTAMP WHERE game_code = 'COD_WARZONE' AND code = 'EVENT';
UPDATE service_categories SET name = '其他任务 / 人工询价', icon_key = 'quote', description = '未列出的战区需求，按截图与目标评估', sort_order = 100, is_active = 1, updated_at = CURRENT_TIMESTAMP WHERE game_code = 'COD_WARZONE' AND code = 'OTHER';
UPDATE service_categories SET name = 'DMZ任务（已迁移至 DMZ 游戏）', is_active = 0, sort_order = 999, updated_at = CURRENT_TIMESTAMP WHERE game_code = 'COD_WARZONE' AND code = 'DMZ';

UPDATE service_categories SET name = 'DMZ任务', is_active = 0, sort_order = 999, updated_at = CURRENT_TIMESTAMP WHERE game_code = 'CODMW4' AND code = 'DMZ';

INSERT INTO game_platforms (game_code, platform_code, is_active, sort_order, created_at, updated_at)
VALUES
('DMZ', 'BATTLE_NET', 1, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DMZ', 'STEAM', 1, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DMZ', 'PLAYSTATION', 1, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DMZ', 'XBOX', 1, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active), sort_order = VALUES(sort_order), updated_at = CURRENT_TIMESTAMP;

UPDATE services SET game_code = 'DMZ', category = 'DMZ_MISSIONS', sort_order = 10, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-missions';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_MISSIONS', sort_order = 20, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-story-missions';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_MISSIONS', sort_order = 30, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-bounty';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_MISSIONS', sort_order = 40, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-lieutenants';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_MISSIONS', sort_order = 50, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-mia-rescue';

UPDATE services SET game_code = 'DMZ', category = 'DMZ_EXTRACTION', sort_order = 10, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'dmz-key-farming';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_EXTRACTION', sort_order = 20, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'dmz-loadout-recovery';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_EXTRACTION', sort_order = 30, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_EXTRACTION', sort_order = 40, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-cash';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_EXTRACTION', sort_order = 50, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-crafting';

UPDATE services SET game_code = 'DMZ', category = 'DMZ_PROGRESSION', sort_order = 10, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'dmz-faction-rep';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_PROGRESSION', sort_order = 20, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-progression';
UPDATE services SET game_code = 'DMZ', category = 'DMZ_PROGRESSION', sort_order = 30, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-leveling';

UPDATE services SET game_code = 'DMZ', category = 'DMZ_WEAPON', sort_order = 10, updated_at = CURRENT_TIMESTAMP, version = version + 1 WHERE slug = 'mw4-dmz-weapon-unlock';
