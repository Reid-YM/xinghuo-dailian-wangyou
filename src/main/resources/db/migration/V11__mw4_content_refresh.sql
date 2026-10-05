UPDATE site_config
SET brand_name = '星火网游',
    service_status = 'OPEN',
    quote_enabled = TRUE,
    order_query_enabled = TRUE,
    hero_title = '使命召唤 MW4 任务代练',
    hero_subtitle = '武器升级 · 迷彩解锁 · 活动任务 · 排位冲分',
    starting_price_mode = 'AUTO',
    starting_price = 20.00,
    home_service_limit = 6,
    trust_tags = '纯绿手打|进度可查|淘宝/闲鱼担保|异常及时沟通',
    announcement_enabled = TRUE,
    announcement_text = '正常接单 · 纯绿手打 · 10:00–24:00',
    wechat_id = 'AAC9878',
    service_hours = '10:00–24:00',
    contact_notice = '网页、查询页、截图都不要发密码/验证码/令牌。确认报价后再走淘宝/闲鱼付款。',
    quote_success_text = '提交成功。业务编号：{id}\n\n1. 复制编号\n2. 加微信 AAC9878，把编号发给客服\n3. 确认报价后再走淘宝/闲鱼付款\n\n可在「业务记录」随时查进度。网页不要发账号密码。',
    seo_title = 'COD MW4 代练 | 武器升级/迷彩/活动任务 ¥20起 | 星火网游',
    seo_description = '使命召唤 MW4 任务代练。武器升级 ¥20 起，迷彩/活动/排位按进度报价。Steam、战网、PS、Xbox、Switch 2 可接。提交需求拿业务编号，微信一对一确认。客服 10:00–24:00。网页不收账号密码。',
    updated_at = UTC_TIMESTAMP(6),
    version = version + 1
WHERE id = 1;

UPDATE game_catalog
SET name = '使命召唤 MW4',
    name_en = 'Call of Duty: Modern Warfare 4',
    aliases = 'COD MW4,MW4,使命召唤现代战争4',
    publisher = 'Activision',
    genres = 'FPS,多人竞技,任务代练',
    is_active = TRUE,
    is_featured = TRUE,
    business_status = 'ACTIVE',
    seo_title = 'COD MW4 代练 | 星火网游',
    seo_description = 'COD MW4 武器升级、迷彩、活动任务、排位冲分服务。',
    updated_at = UTC_TIMESTAMP
WHERE code = 'CODMW4';

UPDATE service_categories
SET is_active = TRUE,
    updated_at = UTC_TIMESTAMP
WHERE game_code = 'CODMW4'
  AND code IN ('ACTIVITY','WEAPON','CAMO','RANK','OTHER');

INSERT INTO services (slug,game_code,name,category,short_desc,price_mode,price_fixed,price_min,price_max,duration_text,status,cover_image,hot_score,manual_priority,is_active,sort_order,published_at,created_at,updated_at,version)
VALUES
('weapon-leveling','CODMW4','武器升级','WEAPON','按目标等级与当前进度报价，配件一并解锁','RANGE',NULL,20.00,60.00,'约 30–90 分钟','AVAILABLE','/assets/covers/mw4-weapon.svg',100.00,60,TRUE,10,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('camo-task','CODMW4','迷彩任务','CAMO','金/钻石/暗物质按剩余挑战确认','QUOTE',NULL,80.00,NULL,'按进度确认','AVAILABLE','/assets/covers/mw4-camo.svg',96.00,50,TRUE,20,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('dmz-task','CODMW4','DMZ 任务','OTHER','DMZ 升级与任务推进','QUOTE',NULL,50.00,NULL,'约 1 天','AVAILABLE','/assets/covers/mw4-dmz.svg',84.00,35,TRUE,30,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('event-task','CODMW4','当前活动','ACTIVITY','限时活动全清，按剩余天数报价','QUOTE',NULL,NULL,NULL,'按活动剩余时间','AVAILABLE','/assets/covers/mw4-event.svg',92.00,45,TRUE,40,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('rank-boost','CODMW4','排位 / 段位','RANK','按当前段位到目标段位报价','QUOTE',NULL,100.00,NULL,'按段差确认','AVAILABLE','/assets/covers/mw4-rank.svg',88.00,42,TRUE,50,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0),
('other-task','CODMW4','其他任务','OTHER','叫不出名字就发截图，人工判断','QUOTE',NULL,NULL,NULL,'人工确认','AVAILABLE','/assets/covers/mw4-other.svg',70.00,20,TRUE,60,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)
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
    cover_image = CASE WHEN slug = 'dmz-task' THEN COALESCE(NULLIF(cover_image,''), VALUES(cover_image)) ELSE VALUES(cover_image) END,
    hot_score = VALUES(hot_score),
    manual_priority = VALUES(manual_priority),
    is_active = VALUES(is_active),
    sort_order = VALUES(sort_order),
    updated_at = UTC_TIMESTAMP(6),
    version = version + 1;
