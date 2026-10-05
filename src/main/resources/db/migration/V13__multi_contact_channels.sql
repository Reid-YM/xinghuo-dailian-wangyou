ALTER TABLE site_config
  ADD COLUMN contact_channels TEXT NULL AFTER wechat_qr_size;

UPDATE site_config
SET contact_channels = CONCAT(
  '主客服', CHAR(9),
  COALESCE(NULLIF(wechat_id, ''), 'AAC9878'), CHAR(9),
  COALESCE(NULLIF(wechat_qr_image, ''), ''), CHAR(9),
  '优先添加，发送业务编号确认报价'
)
WHERE id = 1
  AND (contact_channels IS NULL OR contact_channels = '');
