-- 会话滑动续期（2026-10-13）
--
-- 背景：expires_at 过去等于「签发时间 + 固定 TTL」，用户一直在浏览器里用着，
-- 也会在第 8 小时被强行登出（生产上出现过「刚绑完手机号，网站就要重新登录」）。
--
-- 方案：expires_at 改成「空闲截止时间」——每次活跃把它推到 now + idle_seconds（滑动续期）；
-- 同时用 absolute_expires_at 兜底：会话 id 是持有即可登录的凭据，不能靠定时刷新变成永久有效。
--
-- 既有会话保持原行为：absolute_expires_at 回填为当前 expires_at，
-- 这次升级不会延长任何一条已存在的会话。

ALTER TABLE user_session
    ADD COLUMN idle_seconds INT NOT NULL DEFAULT 28800
        COMMENT '空闲窗口（秒）：活跃时把 expires_at 推到 now + idle_seconds',
    ADD COLUMN absolute_expires_at DATETIME(6) NULL
        COMMENT '绝对上限：无论多活跃，到点必须重新登录';

UPDATE user_session SET absolute_expires_at = expires_at WHERE absolute_expires_at IS NULL;