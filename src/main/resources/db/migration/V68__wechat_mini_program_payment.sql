-- V3H: record the single WeChat prepay channel chosen for a payment intent.
-- Existing Native rows are backfilled from their stored code_url. New columns are nullable
-- so the previous application version can continue running during a rolling deploy.
ALTER TABLE payment_order
  ADD COLUMN wechat_trade_type VARCHAR(20) NULL COMMENT 'NATIVE / MINI_PROGRAM; first prepay channel wins',
  ADD COLUMN wechat_payer_hash CHAR(64) NULL COMMENT 'SHA-256 of mini-program openid used to create JSAPI prepay',
  ADD COLUMN wechat_request_timestamp VARCHAR(20) NULL COMMENT 'wx.requestPayment timeStamp',
  ADD COLUMN wechat_request_nonce VARCHAR(64) NULL COMMENT 'wx.requestPayment nonceStr',
  ADD COLUMN wechat_request_package VARCHAR(128) NULL COMMENT 'wx.requestPayment package',
  ADD COLUMN wechat_request_sign_type VARCHAR(16) NULL COMMENT 'wx.requestPayment signType',
  ADD COLUMN wechat_request_pay_sign VARCHAR(512) NULL COMMENT 'wx.requestPayment paySign';

UPDATE payment_order
   SET wechat_trade_type = 'NATIVE'
 WHERE provider = 'WECHAT'
   AND native_code_url IS NOT NULL
   AND wechat_trade_type IS NULL;
