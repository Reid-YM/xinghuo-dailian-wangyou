-- V21 aligns the public storefront copy, market-facing starting prices and stale test data.

SET @now = UTC_TIMESTAMP(6);

UPDATE site_config
SET brand_name='星火网游代练平台',
    hero_title='使命召唤 MW4 任务代练',
    hero_subtitle='武器升级 · 迷彩解锁 · 活动任务 · 排位冲分',
    seo_title='COD MW4 代练 | 武器升级/迷彩/活动任务 ¥20起 | 星火网游代练平台',
    seo_description='星火网游代练平台提供使命召唤 MW4、战狗、CS2、无畏契约等游戏任务服务。武器升级 ¥20 起，迷彩/活动/排位按进度报价。提交需求拿业务编号，微信一对一确认。网页不收账号密码。',
    quote_success_text='提交成功。业务编号：{id}\n\n1. 复制编号\n2. 加微信 AAC9878，把编号发给客服\n3. 确认报价后再走淘宝/闲鱼付款\n\n可在「业务记录」随时查进度。网页不要发账号密码。',
    updated_at=@now
WHERE id=1;

UPDATE services
SET status='OFFLINE', is_active=FALSE, updated_at=@now
WHERE name LIKE '%测试%' OR slug LIKE '%test%';

UPDATE services
SET name='DMZ / 撤离任务',
    category='DMZ',
    short_desc='DMZ、撤离、阵营任务与物资推进，按清单确认报价',
    price_mode='QUOTE',
    price_fixed=NULL,
    price_min=50.00,
    price_max=NULL,
    duration_text='约 1 天',
    updated_at=@now
WHERE slug='dmz-task';

UPDATE services
SET price_min = CASE
    WHEN slug='weapon-leveling' THEN 20.00
    WHEN slug='camo-task' THEN 80.00
    WHEN slug='rank-boost' THEN 100.00
    WHEN slug='other-task' THEN NULL
    WHEN game_code='WAR_DOGS' AND slug='war-dogs-leveling' THEN 30.00
    WHEN game_code='WAR_DOGS' AND slug='war-dogs-weapon-unlock' THEN 40.00
    WHEN game_code='WAR_DOGS' AND slug='war-dogs-class-mastery' THEN 60.00
    WHEN game_code='WAR_DOGS' AND slug='war-dogs-missions' THEN 50.00
    WHEN game_code='WAR_DOGS' AND slug='war-dogs-squad-objectives' THEN 100.00
    WHEN game_code IN ('VALORANT','CS2','APEX','PUBG','NARAKA','MARVEL_RIVALS','R6_SIEGE','FORTNITE') AND category IN ('RANKED','PREMIER','COMPETITIVE','FACEIT','RANKED_BR') THEN 100.00
    WHEN game_code IN ('LOL','HONOR_KINGS') AND category IN ('RANK_SOLO','RANK_FLEX','RANKED','PEAK') THEN 80.00
    WHEN game_code IN ('TEKKEN8','STREET_FIGHTER6') AND category='RANKED' THEN 100.00
    WHEN game_code IN ('WOW','DESTINY2','EA_FC26') AND category IN ('DUNGEON','RAID','RIVALS','CHAMPIONS','PVP') THEN 120.00
    WHEN game_code IN ('GENSHIN','HONKAI_SR','ZZZ','WUTHERING_WAVES') AND category IN ('DAILY') THEN 20.00
    WHEN game_code IN ('GENSHIN','HONKAI_SR','ZZZ','WUTHERING_WAVES') AND category IN ('MATERIAL','QUEST') THEN 40.00
    WHEN game_code IN ('GENSHIN','HONKAI_SR','ZZZ','WUTHERING_WAVES') AND category IN ('ABYSS','MOC','SHIYU','TOWER') THEN 60.00
    WHEN category IN ('EVENT','ACTIVITY','BATTLEPASS','PASS','QUEST','MISSION','SEASON','EVENT_PASS','WEEKLY') THEN 50.00
    WHEN category IN ('LEVEL','LEVELING','POWER','HUNTER_RANK') THEN 80.00
    WHEN category IN ('GEAR','EQUIPMENT','LOADOUT','WEAPON','OPERATOR','MATERIAL','FARM') THEN 60.00
    ELSE price_min
  END,
  price_max = CASE
    WHEN slug='weapon-leveling' THEN 60.00
    WHEN game_code='WAR_DOGS' AND slug='war-dogs-leveling' THEN 120.00
    WHEN game_code='WAR_DOGS' AND slug='war-dogs-weapon-unlock' THEN 160.00
    WHEN price_mode='RANGE' AND category IN ('DAILY','WEEKLY') THEN 80.00
    ELSE price_max
  END,
  price_fixed = CASE WHEN price_mode='FIXED' THEN price_fixed ELSE NULL END,
  updated_at=@now
WHERE is_active=TRUE;

UPDATE services
SET price_mode='RANGE', updated_at=@now
WHERE slug IN ('weapon-leveling','war-dogs-leveling','war-dogs-weapon-unlock')
  AND is_active=TRUE;

UPDATE services
SET price_mode='QUOTE', price_max=NULL, updated_at=@now
WHERE is_active=TRUE
  AND slug NOT IN ('weapon-leveling','war-dogs-leveling','war-dogs-weapon-unlock')
  AND price_mode <> 'FIXED'
  AND category NOT IN ('DAILY','WEEKLY');

UPDATE services
SET price_mode='RANGE', price_max=COALESCE(price_max, 80.00), updated_at=@now
WHERE is_active=TRUE
  AND category IN ('DAILY','WEEKLY')
  AND price_mode <> 'FIXED';

UPDATE game_catalog
SET seo_title=CONCAT(name, '任务服务 | 星火网游代练平台'),
    seo_description=CONCAT(name, '游戏任务服务。排位、活动、等级、材料或截图需求按进度报价；提交需求拿业务编号，客服一对一确认。网页不收账号密码。'),
    updated_at=@now
WHERE is_active=TRUE AND (seo_title IS NULL OR seo_title NOT LIKE '%星火网游代练平台%');
