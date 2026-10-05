-- V18 keeps the game library useful for operations: games with clear platform
-- coverage and enough metadata are enabled as READY so admins can list services
-- or accept manual quote requests without rebuilding the catalog first.

UPDATE game_catalog
SET is_active = TRUE,
    business_status = 'READY',
    is_featured = code IN ('CS2','VALORANT','APEX','DELTA_FORCE','GENSHIN','LOL','FORTNITE','MARVEL_RIVALS'),
    seo_title = CONCAT(name, ' 代练/任务/进度服务 | 星火网游'),
    seo_description = CONCAT(name, ' 游戏服务支持等级、任务、活动、排位或收集类需求。提交需求后人工确认报价，网页不收账号密码。'),
    updated_at = CURRENT_TIMESTAMP
WHERE code IN (
    'CS2','VALORANT','APEX','DELTA_FORCE','FORTNITE','LOL','GTA5_ONLINE','GENSHIN',
    'MARVEL_RIVALS','R6_SIEGE','WOW','DIABLO4','WUTHERING_WAVES','HONKAI_SR','ZZZ',
    'ARC_RAIDERS','PUBG','WARFRAME','DESTINY2','MH_WILDS','TEKKEN8','STREET_FIGHTER6',
    'NARAKA','EA_FC26','HONOR_KINGS','COD_WARZONE'
);

UPDATE game_catalog
SET business_status = 'ACTIVE',
    is_active = TRUE,
    is_featured = TRUE,
    seo_title = 'COD MW4 代练 | 武器升级/迷彩/活动任务 ¥20起 | 星火网游',
    seo_description = '使命召唤 MW4 任务代练。武器升级、迷彩、活动任务、排位按进度报价。Steam、战网、PS、Xbox、Switch 2 可接。网页不收账号密码。',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'CODMW4';

UPDATE game_catalog
SET publisher = 'Tencent',
    genres = 'MOBA,移动竞技,排位,活动',
    name_en = 'Honor of Kings',
    aliases = '王者荣耀,HOK',
    popularity_score = 94,
    seo_slug = 'honor-of-kings',
    seo_title = '王者荣耀代练/排位/活动任务 | 星火网游',
    seo_description = '王者荣耀排位、活动任务、等级进度等服务支持人工报价。提交需求后客服确认，不在网页提交账号密码。',
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'HONOR_KINGS';

UPDATE game_catalog
SET business_status = 'PLANNED',
    is_active = FALSE,
    is_featured = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE code IN ('COD_BO7','BATTLEFIELD6','NBA2K27','MADDEN27')
  AND (seo_title IS NULL OR seo_description IS NULL);
