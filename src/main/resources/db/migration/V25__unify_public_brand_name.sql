-- Keep the public brand consistent in site configuration and generated game SEO.
SET @now = UTC_TIMESTAMP(6);

UPDATE site_config
SET brand_name = '星火网游代练平台',
    seo_title = CASE
      WHEN seo_title IS NULL OR seo_title = '' THEN '游戏任务服务 | 星火网游代练平台'
      WHEN seo_title NOT LIKE '%星火网游代练平台%' THEN REPLACE(seo_title, '星火网游', '星火网游代练平台')
      ELSE seo_title
    END,
    seo_description = CASE
      WHEN seo_description IS NULL OR seo_description = '' THEN '星火网游代练平台提供多游戏任务服务，按进度报价，网页不收账号密码。'
      WHEN seo_description NOT LIKE '%星火网游代练平台%' THEN REPLACE(seo_description, '星火网游', '星火网游代练平台')
      ELSE seo_description
    END,
    updated_at = @now
WHERE id = 1;

UPDATE game_catalog
SET seo_title = CASE
      WHEN seo_title IS NULL OR seo_title = '' THEN CONCAT(name, ' 任务服务 | 星火网游代练平台')
      WHEN seo_title NOT LIKE '%星火网游代练平台%' THEN REPLACE(seo_title, '星火网游', '星火网游代练平台')
      ELSE seo_title
    END,
    seo_description = CASE
      WHEN seo_description IS NULL OR seo_description = '' THEN CONCAT(name, '游戏任务服务，按进度报价，网页不收账号密码。')
      WHEN seo_description NOT LIKE '%星火网游代练平台%' THEN REPLACE(seo_description, '星火网游', '星火网游代练平台')
      ELSE seo_description
    END,
    updated_at = @now
WHERE is_active = TRUE;
