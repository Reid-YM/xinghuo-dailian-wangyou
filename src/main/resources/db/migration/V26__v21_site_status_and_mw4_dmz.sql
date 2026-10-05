-- V21 (一次性开发): 站点运营状态系统 + MW4 DMZ 服务页 + 状态文案字段
-- 目的：MW4 未正式上线前不再显示"正常接单"，由后台统一控制预约/开放/暂停状态。

SET @now = UTC_TIMESTAMP(6);

-- 1) 站点运营状态：新增"状态短文案"与"主按钮文案"，支持 5 档状态
ALTER TABLE site_config
    ADD COLUMN status_short_text   VARCHAR(120) NULL AFTER service_status,
    ADD COLUMN primary_button_label VARCHAR(40)  NULL AFTER status_short_text;

-- 2) 默认进入"预约中"（RESERVATION），不再使用"正常接单"
UPDATE site_config
SET service_status       = 'RESERVATION',
    status_short_text    = 'MW4 正式版预约中 · 上线后陆续开放服务',
    primary_button_label = '提前询价',
    announcement_enabled = 1,
    announcement_text    = 'MW4 正式版预约中 · 现可提前锁定档期，上线后按顺序开工',
    hero_title           = '使命召唤 MW4 代练服务',
    hero_subtitle        = '武器升级 · 迷彩挑战 · DMZ · 活动任务',
    updated_at           = @now
WHERE id = 1;

-- 3) 补齐 CODMW4 的 DMZ 二级分类，使 DMZ 服务页可被收录
INSERT INTO service_categories (game_code, code, name, icon_key, description, is_active, sort_order, created_at, updated_at)
SELECT 'CODMW4', 'DMZ', 'DMZ 撤离任务', 'service', 'DMZ 撤离、封锁区任务与物资目标推进', 1, 25, @now, @now
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM service_categories WHERE game_code = 'CODMW4' AND code = 'DMZ');

-- 4) DMZ 服务页规范化为可索引的独立落地页
UPDATE services
SET slug         = 'mw4-dmz',
    name         = 'DMZ 撤离任务',
    category     = 'DMZ',
    short_desc   = 'DMZ 撤离、封锁区与物资目标推进，按剩余任务报价',
    duration_text = '约 1 天',
    status       = 'AVAILABLE',
    cover_image  = '/assets/covers/mw4-dmz.svg',
    is_active    = 1,
    sort_order   = 30,
    updated_at   = @now
WHERE game_code = 'CODMW4' AND slug IN ('dmz-task', 'mw4-dmz');

-- 5) 其他任务：补齐展示信息，作为人工询价入口
UPDATE services
SET name         = '其他任务 / 人工询价',
    short_desc   = '未列出的 MW4 需求可走人工评估，按截图与目标报价',
    duration_text = '人工确认',
    status       = 'AVAILABLE',
    is_active    = 1,
    sort_order   = 60,
    updated_at   = @now
WHERE game_code = 'CODMW4' AND slug = 'other-task';

-- 6) 迷彩 / 活动 / 武器：统一展示文案与排序，确保 5 个首批服务页完整
UPDATE services SET sort_order = 10, updated_at = @now WHERE game_code = 'CODMW4' AND slug = 'weapon-leveling';
UPDATE services SET sort_order = 20, updated_at = @now WHERE game_code = 'CODMW4' AND slug = 'camo-task';
UPDATE services SET sort_order = 40, updated_at = @now WHERE game_code = 'CODMW4' AND slug = 'event-task';