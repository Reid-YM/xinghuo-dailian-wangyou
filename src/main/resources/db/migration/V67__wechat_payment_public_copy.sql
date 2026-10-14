-- Replace only the earlier default payment wording. Keep custom site copy intact.
-- The new text remains accurate while WeChat Pay is disabled because the QR
-- option is available only after staff opens payment for an account-linked order.
UPDATE site_config
SET contact_notice = CASE
        WHEN contact_notice = '网页、查询页、截图都不要发密码/验证码/令牌。确认报价后再走淘宝/闲鱼付款。'
        THEN '网页、查询页、截图都不要发密码/验证码/令牌。客服确认金额后，可走淘宝/闲鱼担保；官网订单开放支付后也可在个人中心微信扫码。'
        ELSE contact_notice END,
    quote_success_text = REPLACE(
        quote_success_text,
        '确认报价后再走淘宝/闲鱼付款',
        '客服确认金额后可走淘宝/闲鱼担保；官网订单开放支付后可在个人中心微信扫码'),
    trust_faq = REPLACE(
        trust_faq,
        CONCAT('怎么付款？', CHAR(9), '确认报价后走淘宝或闲鱼担保，网页不收钱。'),
        CONCAT('怎么付款？', CHAR(9), '客服确认金额后，可选择淘宝、闲鱼担保；如订单在个人中心显示微信支付二维码，也可扫码支付该订单。')),
    updated_at = CURRENT_TIMESTAMP(6),
    version = version + 1
WHERE id = 1
  AND (
      contact_notice = '网页、查询页、截图都不要发密码/验证码/令牌。确认报价后再走淘宝/闲鱼付款。'
      OR LOCATE('确认报价后再走淘宝/闲鱼付款', quote_success_text) > 0
      OR LOCATE(CONCAT('怎么付款？', CHAR(9), '确认报价后走淘宝或闲鱼担保，网页不收钱。'), trust_faq) > 0
  );
