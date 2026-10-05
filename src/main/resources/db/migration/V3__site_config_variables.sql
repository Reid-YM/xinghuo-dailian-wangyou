ALTER TABLE site_config
  ADD COLUMN brand_name VARCHAR(80) NOT NULL DEFAULT '星火网游' AFTER id,
  ADD COLUMN quote_enabled BOOLEAN NOT NULL DEFAULT TRUE AFTER service_status,
  ADD COLUMN order_query_enabled BOOLEAN NOT NULL DEFAULT TRUE AFTER quote_enabled,
  ADD COLUMN starting_price_mode VARCHAR(16) NOT NULL DEFAULT 'MANUAL' AFTER hero_subtitle,
  ADD COLUMN home_service_limit INT NOT NULL DEFAULT 4 AFTER starting_price,
  ADD COLUMN announcement_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER trust_tags,
  ADD COLUMN announcement_text VARCHAR(240) NULL AFTER announcement_enabled,
  ADD COLUMN contact_title VARCHAR(80) NOT NULL DEFAULT '联系客服' AFTER announcement_text,
  ADD COLUMN contact_intro VARCHAR(300) NOT NULL DEFAULT '询价后建议发送询价编号；已有订单请直接发送订单号。' AFTER contact_title,
  ADD COLUMN wechat_qr_image VARCHAR(500) NULL AFTER wechat_id,
  ADD COLUMN qq_link VARCHAR(500) NULL AFTER qq_id,
  ADD COLUMN phone VARCHAR(50) NULL AFTER qq_link,
  ADD COLUMN service_hours VARCHAR(100) NULL AFTER phone,
  ADD COLUMN taobao_url VARCHAR(500) NULL AFTER service_hours,
  ADD COLUMN xianyu_url VARCHAR(500) NULL AFTER taobao_url,
  ADD COLUMN contact_notice VARCHAR(500) NOT NULL DEFAULT '请勿在网页询价、订单查询或客服截图中提交账号密码、令牌等敏感凭据。' AFTER xianyu_url,
  ADD COLUMN quote_success_text VARCHAR(300) NOT NULL DEFAULT '提交成功，询价编号：{id}。请联系客服确认最终报价。' AFTER contact_notice,
  ADD COLUMN order_help_text VARCHAR(300) NOT NULL DEFAULT '输入客服提供的订单号，查看当前状态、进度和最近更新。' AFTER quote_success_text,
  ADD COLUMN footer_text VARCHAR(200) NOT NULL DEFAULT '前台仅展示客户需要的信息' AFTER order_help_text,
  ADD COLUMN seo_title VARCHAR(160) NULL AFTER footer_text,
  ADD COLUMN seo_description VARCHAR(300) NULL AFTER seo_title;

UPDATE site_config
SET brand_name = COALESCE(NULLIF(brand_name,''), '星火网游'),
    starting_price_mode = COALESCE(NULLIF(starting_price_mode,''), 'MANUAL'),
    home_service_limit = CASE WHEN home_service_limit BETWEEN 2 AND 8 THEN home_service_limit ELSE 4 END,
    contact_title = COALESCE(NULLIF(contact_title,''), '联系客服'),
    contact_intro = COALESCE(NULLIF(contact_intro,''), '询价后建议发送询价编号；已有订单请直接发送订单号。'),
    contact_notice = COALESCE(NULLIF(contact_notice,''), '请勿在网页询价、订单查询或客服截图中提交账号密码、令牌等敏感凭据。'),
    quote_success_text = COALESCE(NULLIF(quote_success_text,''), '提交成功，询价编号：{id}。请联系客服确认最终报价。'),
    order_help_text = COALESCE(NULLIF(order_help_text,''), '输入客服提供的订单号，查看当前状态、进度和最近更新。'),
    footer_text = COALESCE(NULLIF(footer_text,''), '前台仅展示客户需要的信息')
WHERE id = 1;
