ALTER TABLE game_catalog
  ADD COLUMN name_en VARCHAR(120) NULL AFTER name,
  ADD COLUMN aliases VARCHAR(240) NULL AFTER name_en,
  ADD COLUMN publisher VARCHAR(120) NULL AFTER description,
  ADD COLUMN genres VARCHAR(160) NULL AFTER publisher,
  ADD COLUMN logo_image VARCHAR(500) NULL AFTER genres,
  ADD COLUMN cover_image VARCHAR(500) NULL AFTER logo_image,
  ADD COLUMN popularity_score INT NOT NULL DEFAULT 50 AFTER cover_image,
  ADD COLUMN is_featured BOOLEAN NOT NULL DEFAULT FALSE AFTER popularity_score,
  ADD COLUMN business_status VARCHAR(24) NOT NULL DEFAULT 'PLANNED' AFTER is_featured,
  ADD COLUMN seo_slug VARCHAR(120) NULL AFTER business_status,
  ADD COLUMN seo_title VARCHAR(160) NULL AFTER seo_slug,
  ADD COLUMN seo_description VARCHAR(300) NULL AFTER seo_title;

ALTER TABLE platform_catalog
  ADD COLUMN icon_key VARCHAR(40) NULL AFTER name,
  ADD COLUMN platform_group VARCHAR(32) NULL AFTER icon_key;

CREATE INDEX idx_game_catalog_active_featured ON game_catalog(is_active, is_featured, popularity_score, sort_order);
CREATE INDEX idx_game_catalog_business_status ON game_catalog(business_status, popularity_score);

UPDATE platform_catalog SET icon_key='desktop', platform_group='PC' WHERE code IN ('STEAM','EPIC','BATTLE_NET','RIOT','EA_APP','UBISOFT','ROCKSTAR','WEGAME','HOYOPLAY','OFFICIAL');
UPDATE platform_catalog SET icon_key='console', platform_group='CONSOLE' WHERE code IN ('PLAYSTATION','XBOX','SWITCH','SWITCH2');
UPDATE platform_catalog SET icon_key='mobile', platform_group='MOBILE' WHERE code IN ('IOS','ANDROID');

UPDATE game_catalog SET
  name_en='Call of Duty: Modern Warfare 4', aliases='COD MW4,MW4,使命召唤现代战争4', publisher='Activision', genres='FPS,多人竞技,任务服务',
  popularity_score=100, is_featured=TRUE, business_status='ACTIVE', seo_slug='cod-mw4',
  seo_title='COD MW4 任务服务 - 星火网游', seo_description='COD MW4 武器、迷彩、活动与排位等任务服务。'
WHERE code='CODMW4';

UPDATE game_catalog SET popularity_score=96, publisher='Valve', genres='FPS,战术竞技,排位', name_en='Counter-Strike 2', aliases='CS2,Counter Strike 2', seo_slug='cs2' WHERE code='CS2';
UPDATE game_catalog SET popularity_score=95, publisher='Riot Games', genres='FPS,战术竞技,排位', name_en='VALORANT', aliases='无畏契约,瓦罗兰特', seo_slug='valorant' WHERE code='VALORANT';
UPDATE game_catalog SET popularity_score=94, publisher='Electronic Arts', genres='FPS,战术竞技,排位', name_en='Apex Legends', aliases='Apex,APEX英雄', seo_slug='apex-legends' WHERE code='APEX';
UPDATE game_catalog SET popularity_score=94, publisher='Tencent', genres='FPS,撤离射击,多人竞技', name_en='Delta Force', aliases='三角洲行动,DF', seo_slug='delta-force' WHERE code='DELTA_FORCE';
UPDATE game_catalog SET popularity_score=93, publisher='Epic Games', genres='大逃杀,多人竞技,活动', name_en='Fortnite', aliases='堡垒之夜', seo_slug='fortnite' WHERE code='FORTNITE';
UPDATE game_catalog SET popularity_score=93, publisher='Riot Games', genres='MOBA,排位,活动', name_en='League of Legends', aliases='英雄联盟,LOL', seo_slug='league-of-legends' WHERE code='LOL';
UPDATE game_catalog SET popularity_score=92, publisher='Rockstar Games', genres='开放世界,多人,在线进度', name_en='Grand Theft Auto Online', aliases='GTA Online,GTA5 Online', seo_slug='gta-online' WHERE code='GTA5_ONLINE';
UPDATE game_catalog SET popularity_score=92, publisher='HoYoverse', genres='开放世界,RPG,探索收集', name_en='Genshin Impact', aliases='原神,Genshin', seo_slug='genshin-impact' WHERE code='GENSHIN';
UPDATE game_catalog SET popularity_score=91, publisher='NetEase Games', genres='英雄射击,多人竞技,排位', name_en='Marvel Rivals', aliases='漫威争锋', seo_slug='marvel-rivals' WHERE code='MARVEL_RIVALS';
UPDATE game_catalog SET popularity_score=90, publisher='Ubisoft', genres='FPS,战术竞技,排位', name_en='Tom Clancy''s Rainbow Six Siege', aliases='彩虹六号,R6,R6S', seo_slug='rainbow-six-siege' WHERE code='R6_SIEGE';
UPDATE game_catalog SET popularity_score=90, publisher='Blizzard Entertainment', genres='MMORPG,副本,等级,收集', name_en='World of Warcraft', aliases='魔兽世界,WOW', seo_slug='world-of-warcraft' WHERE code='WOW';
UPDATE game_catalog SET popularity_score=89, publisher='Blizzard Entertainment', genres='ARPG,赛季,装备,等级', name_en='Diablo IV', aliases='暗黑4,Diablo 4,D4', seo_slug='diablo-4' WHERE code='DIABLO4';
UPDATE game_catalog SET popularity_score=89, publisher='Kuro Games', genres='开放世界,动作RPG,养成', name_en='Wuthering Waves', aliases='鸣潮,Wuwa', seo_slug='wuthering-waves' WHERE code='WUTHERING_WAVES';
UPDATE game_catalog SET popularity_score=88, publisher='HoYoverse', genres='回合制RPG,养成,活动', name_en='Honkai: Star Rail', aliases='崩坏星穹铁道,星铁,HSR', seo_slug='honkai-star-rail' WHERE code='HONKAI_SR';
UPDATE game_catalog SET popularity_score=88, publisher='HoYoverse', genres='动作RPG,养成,活动', name_en='Zenless Zone Zero', aliases='绝区零,ZZZ', seo_slug='zenless-zone-zero' WHERE code='ZZZ';
UPDATE game_catalog SET popularity_score=88, publisher='Embark Studios', genres='撤离射击,PvPvE,装备进度', name_en='ARC Raiders', aliases='ARC,Arc Raiders', seo_slug='arc-raiders' WHERE code='ARC_RAIDERS';
UPDATE game_catalog SET popularity_score=87, publisher='KRAFTON', genres='大逃杀,多人竞技,生存', name_en='PUBG: BATTLEGROUNDS', aliases='PUBG,绝地求生', seo_slug='pubg' WHERE code='PUBG';
UPDATE game_catalog SET popularity_score=87, publisher='Digital Extremes', genres='合作射击,刷取,养成', name_en='Warframe', aliases='星际战甲,Warframe', seo_slug='warframe' WHERE code='WARFRAME';
UPDATE game_catalog SET popularity_score=86, publisher='Bungie', genres='多人射击,副本,赛季', name_en='Destiny 2', aliases='命运2,D2', seo_slug='destiny-2' WHERE code='DESTINY2';
UPDATE game_catalog SET popularity_score=85, publisher='Capcom', genres='动作RPG,共斗,装备', name_en='Monster Hunter Wilds', aliases='怪物猎人荒野,MH Wilds', seo_slug='monster-hunter-wilds' WHERE code='MH_WILDS';
UPDATE game_catalog SET popularity_score=85, publisher='Bandai Namco', genres='格斗,排位,竞技', name_en='TEKKEN 8', aliases='铁拳8,T8', seo_slug='tekken-8' WHERE code='TEKKEN8';
UPDATE game_catalog SET popularity_score=85, publisher='Capcom', genres='格斗,排位,竞技', name_en='Street Fighter 6', aliases='街霸6,SF6', seo_slug='street-fighter-6' WHERE code='STREET_FIGHTER6';
UPDATE game_catalog SET popularity_score=84, publisher='24 Entertainment', genres='动作竞技,排位,赛季', name_en='NARAKA: BLADEPOINT', aliases='永劫无间,Naraka', seo_slug='naraka-bladepoint' WHERE code='NARAKA';
UPDATE game_catalog SET popularity_score=84, publisher='Electronic Arts', genres='足球,竞技,Ultimate Team', name_en='EA SPORTS FC 26', aliases='FC26,FIFA', seo_slug='ea-sports-fc-26' WHERE code='EA_FC26';

UPDATE game_catalog
SET business_status=CASE WHEN is_active THEN 'ACTIVE' ELSE 'PLANNED' END,
    seo_slug=COALESCE(seo_slug, LOWER(REPLACE(code,'_','-'))),
    name_en=COALESCE(name_en, name)
WHERE business_status IS NULL OR business_status='PLANNED' OR seo_slug IS NULL OR name_en IS NULL;
