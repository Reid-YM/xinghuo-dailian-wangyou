-- 企业微信客服入口改为后台可配置：链接与说明文案。
-- 之前链接与文案写死在 contact / quote / console / 全站悬浮按钮模板里，运营无法调整。
ALTER TABLE site_config
  ADD COLUMN wecom_kefu_url VARCHAR(500) NULL AFTER contact_notice,
  ADD COLUMN wecom_kefu_note VARCHAR(300) NULL AFTER wecom_kefu_url;

UPDATE site_config
SET wecom_kefu_url = COALESCE(NULLIF(wecom_kefu_url, ''), 'https://work.weixin.qq.com/kfid/kfc80cfa9ee45840208'),
    wecom_kefu_note = COALESCE(NULLIF(wecom_kefu_note, ''), '点击后打开企业微信客服会话，发送业务编号或截图说明需求。');
