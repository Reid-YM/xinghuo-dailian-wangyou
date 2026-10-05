-- 前台客服卡片上写死的文案改为后台可配置。
-- 涉及：首页/询价页客服卡片标题、淘宝与闲鱼入口文案、企微卡片标题与按钮文字。
ALTER TABLE site_config
  ADD COLUMN quick_contact_title VARCHAR(40) NULL AFTER wecom_kefu_note,
  ADD COLUMN taobao_label VARCHAR(40) NULL AFTER quick_contact_title,
  ADD COLUMN xianyu_label VARCHAR(40) NULL AFTER taobao_label,
  ADD COLUMN wecom_kefu_label VARCHAR(40) NULL AFTER xianyu_label,
  ADD COLUMN wecom_kefu_button VARCHAR(40) NULL AFTER wecom_kefu_label;

UPDATE site_config
SET quick_contact_title = COALESCE(NULLIF(quick_contact_title, ''), '客服联系'),
    taobao_label        = COALESCE(NULLIF(taobao_label, ''), '店铺入口'),
    xianyu_label        = COALESCE(NULLIF(xianyu_label, ''), '主页入口'),
    wecom_kefu_label    = COALESCE(NULLIF(wecom_kefu_label, ''), '企业微信客服'),
    wecom_kefu_button   = COALESCE(NULLIF(wecom_kefu_button, ''), '联系客服');
