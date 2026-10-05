SET @now = UTC_TIMESTAMP(6);

SET @add_contact_method = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE quotes ADD COLUMN contact_method VARCHAR(40) NULL AFTER expected_time',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'quotes' AND column_name = 'contact_method'
);
PREPARE stmt FROM @add_contact_method;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_customer_contact = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE quotes ADD COLUMN customer_contact VARCHAR(120) NULL AFTER contact_method',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'quotes' AND column_name = 'customer_contact'
);
PREPARE stmt FROM @add_customer_contact;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE site_config
SET hero_title='多游戏平台任务服务 · MW4 主推',
    hero_subtitle='COD MW4 · 战狗 · CS2 · 无畏契约 · 武器/等级/活动/排位',
    seo_title='星火网游 | COD MW4/战狗/CS2/无畏契约等游戏任务服务 ¥20起',
    seo_description='星火网游提供 COD MW4、战狗、CS2、无畏契约、三角洲行动、王者荣耀、原神等游戏任务服务。武器、等级、活动、排位按进度报价；提交需求拿业务编号，客服一对一确认。网页不收账号密码。',
    updated_at=@now
WHERE id=1;

INSERT IGNORE INTO game_catalog(code,name,name_en,aliases,icon_key,description,publisher,genres,logo_image,cover_image,popularity_score,is_featured,business_status,seo_slug,seo_title,seo_description,is_active,sort_order,created_at,updated_at) VALUES
('WAR_DOGS','战狗','War Dogs','战狗,War Dogs,战地类,大战场射击','gamepad','大规模战术 FPS / 军事沙盒，适合等级、兵种、武器、活动任务与协同目标服务。','Bulkhead','FPS,大战场,战术射击,军事沙盒',NULL,'/assets/covers/war-dogs.svg',97,TRUE,'ACTIVE','war-dogs','战狗代练 | 等级/兵种/武器/活动任务 | 星火网游','战狗游戏任务服务。等级推进、兵种熟练、武器配件、活动任务、战绩协同按目标报价；Steam 可接，提交需求拿业务编号，客服一对一确认。',TRUE,12,@now,@now);

INSERT IGNORE INTO game_platforms(game_code,platform_code,is_active,sort_order,created_at,updated_at) VALUES
('WAR_DOGS','STEAM',TRUE,10,@now,@now);

INSERT INTO service_categories(game_code,code,name,icon_key,description,is_active,sort_order,created_at,updated_at) VALUES
('WAR_DOGS','LEVEL','等级 / 基础进度','rank','账号等级、基础解锁、赛季经验与通用进度',TRUE,10,@now,@now),
('WAR_DOGS','CLASS','兵种 / 职能熟练','star','突击、支援、侦察、载具等职能熟练与目标进度',TRUE,20,@now,@now),
('WAR_DOGS','WEAPON','武器 / 配件解锁','weapon','指定武器等级、配件、常用配置与熟练度',TRUE,30,@now,@now),
('WAR_DOGS','MISSION','任务 / 活动挑战','event','每日周常、赛季活动、限时挑战与清单任务',TRUE,40,@now,@now),
('WAR_DOGS','SQUAD','组队协同目标','service','胜场、战绩、模式目标与人工组队协同',TRUE,50,@now,@now),
('WAR_DOGS','OTHER','截图估价','quote','叫不出名字的需求发截图，人工判断报价',TRUE,60,@now,@now)
ON DUPLICATE KEY UPDATE name=VALUES(name), icon_key=VALUES(icon_key), description=VALUES(description), is_active=VALUES(is_active), sort_order=VALUES(sort_order), updated_at=VALUES(updated_at);

INSERT INTO services(slug,game_code,name,category,short_desc,price_mode,price_fixed,price_min,price_max,duration_text,status,cover_image,hot_score,manual_priority,published_at,is_active,sort_order,created_at,updated_at,version) VALUES
('war-dogs-leveling','WAR_DOGS','战狗等级 / 基础进度','LEVEL','账号等级、赛季经验与基础解锁推进','RANGE',NULL,30.00,120.00,'约 1–3 小时','AVAILABLE','/assets/covers/war-dogs.svg',96.00,22,@now,TRUE,10,@now,@now,0),
('war-dogs-class-mastery','WAR_DOGS','兵种 / 职能熟练','CLASS','按指定兵种、职能目标和当前进度报价','QUOTE',NULL,60.00,NULL,'按目标确认','AVAILABLE','/assets/covers/generic-rank.svg',88.00,16,@now,TRUE,20,@now,@now,0),
('war-dogs-weapon-unlock','WAR_DOGS','武器 / 配件解锁','WEAPON','指定武器等级、配件和常用配置解锁','RANGE',NULL,40.00,160.00,'约 1–6 小时','AVAILABLE','/assets/covers/generic-fps.svg',90.00,18,@now,TRUE,30,@now,@now,0),
('war-dogs-missions','WAR_DOGS','任务 / 活动挑战','MISSION','每日周常、限时活动和赛季挑战清单推进','QUOTE',NULL,50.00,NULL,'按任务清单确认','AVAILABLE','/assets/covers/generic-event.svg',84.00,12,@now,TRUE,40,@now,@now,0),
('war-dogs-squad-objectives','WAR_DOGS','组队协同目标','SQUAD','胜场、模式目标和战绩类需求人工评估','QUOTE',NULL,100.00,NULL,'人工确认','AVAILABLE','/assets/covers/generic-fps.svg',78.00,8,@now,TRUE,50,@now,@now,0)
ON DUPLICATE KEY UPDATE game_code=VALUES(game_code), name=VALUES(name), category=VALUES(category), short_desc=VALUES(short_desc), price_mode=VALUES(price_mode), price_fixed=VALUES(price_fixed), price_min=VALUES(price_min), price_max=VALUES(price_max), duration_text=VALUES(duration_text), status=VALUES(status), cover_image=VALUES(cover_image), hot_score=VALUES(hot_score), manual_priority=VALUES(manual_priority), is_active=VALUES(is_active), sort_order=VALUES(sort_order), updated_at=VALUES(updated_at);

UPDATE services SET cover_image = CASE
  WHEN category IN ('RANK','RANKED','RANK_SOLO','RANK_FLEX','PREMIER','COMPETITIVE','FACEIT','RANKED_BR','ABYSS','MOC','SHIYU','TOWER','DUNGEON','PVP','HUNTER_RANK','PEAK','RIVALS','CHAMPIONS') THEN '/assets/covers/generic-rank.svg'
  WHEN category IN ('EVENT','ACTIVITY','BATTLEPASS','PASS','QUEST','MISSION','SEASON','EVENT_PASS','DAILY','WEEKLY','RAID') THEN '/assets/covers/generic-event.svg'
  WHEN category IN ('WEAPON','LOADOUT','GEAR','EQUIPMENT','OPERATOR') THEN '/assets/covers/generic-fps.svg'
  WHEN game_code IN ('GENSHIN','HONKAI_SR','ZZZ','WUTHERING_WAVES') THEN '/assets/covers/generic-rpg.svg'
  ELSE '/assets/covers/generic-fps.svg'
END
WHERE cover_image IS NULL OR cover_image = '';

UPDATE services SET price_min=80.00 WHERE game_code IN ('CS2','VALORANT','APEX','PUBG','NARAKA','MARVEL_RIVALS','R6_SIEGE') AND price_mode='QUOTE' AND category LIKE '%RANK%';
UPDATE services SET price_min=100.00 WHERE game_code IN ('LOL','HONOR_KINGS') AND price_mode='QUOTE' AND category IN ('RANK_SOLO','RANKED','PEAK');
UPDATE services SET price_min=20.00, price_max=80.00 WHERE price_mode='RANGE' AND category IN ('DAILY','WEEKLY') AND (price_min IS NULL OR price_min < 20.00);
UPDATE services SET price_min=50.00 WHERE price_mode='QUOTE' AND category IN ('EVENT','ACTIVITY','BATTLEPASS','PASS','QUEST','MISSION','SEASON','EVENT_PASS') AND (price_min IS NULL OR price_min < 50.00);
