ALTER TABLE game_catalog
    ADD COLUMN icon_key VARCHAR(48) NOT NULL DEFAULT 'gamepad' AFTER name,
    ADD COLUMN description VARCHAR(240) NULL AFTER icon_key;

ALTER TABLE service_categories
    ADD COLUMN icon_key VARCHAR(48) NOT NULL DEFAULT 'layers' AFTER name,
    ADD COLUMN description VARCHAR(240) NULL AFTER icon_key;

UPDATE game_catalog
SET icon_key='gamepad', description=COALESCE(description, '游戏与版本维度，用于一级业务归属')
WHERE code='CODMW4';

UPDATE service_categories
SET icon_key = CASE code
    WHEN 'ACTIVITY' THEN 'calendar'
    WHEN 'WEAPON' THEN 'crosshair'
    WHEN 'CAMO' THEN 'palette'
    WHEN 'RANK' THEN 'trophy'
    ELSE 'layers'
END,
description = CASE code
    WHEN 'ACTIVITY' THEN '活动、赛季、限时任务与奖励相关服务'
    WHEN 'WEAPON' THEN '武器等级、配件与相关解锁服务'
    WHEN 'CAMO' THEN '迷彩进度、挑战与解锁相关服务'
    WHEN 'RANK' THEN '排位、段位与竞技目标相关服务'
    ELSE '其他需要人工确认的任务服务'
END
WHERE game_code='CODMW4';
