-- Additive Native QR metadata. Existing payment rows remain valid.
ALTER TABLE payment_order
  ADD COLUMN native_code_url VARCHAR(512) NULL COMMENT '微信 Native code_url；只返回订单归属账号',
  ADD COLUMN native_expires_at DATETIME(6) NULL COMMENT '二维码截止时间';
