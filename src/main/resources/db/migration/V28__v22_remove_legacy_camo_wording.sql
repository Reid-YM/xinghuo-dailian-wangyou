-- V22 P0-07：清除旧迷彩代称（金/钻石/暗物质），改为模式/目标泛化描述。
-- 正式版名称以上线后的官方信息为准，避免写死过时称呼。
SET @now = UTC_TIMESTAMP(6);

UPDATE services
SET short_desc = '按剩余迷彩挑战与目标模式确认，正式名称以官方公布为准',
    updated_at = @now
WHERE slug = 'camo-task';

UPDATE site_config
SET trust_cases = REPLACE(
        REPLACE(trust_cases, '金/钻石/暗物质按剩余项目确认，网页不收密码。', '按剩余迷彩挑战项目确认，正式名称以官方公布为准。'),
        '金/钻石/暗物质', '迷彩系列'),
    updated_at = @now
WHERE trust_cases LIKE '%暗物质%';
