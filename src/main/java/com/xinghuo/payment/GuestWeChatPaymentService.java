package com.xinghuo.payment;

import com.xinghuo.exception.ConflictException;

import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guest side of the existing WeChat Native payment rail.
 *
 * <p>The webhook and scheduled reconciliation stay in {@link WeChatNativePaymentService}; both
 * operate on payment_order by payment number and therefore also cover rows whose owner is a
 * guest_business_access rather than a registered customer account.
 */
@Service
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "enabled", havingValue = "true")
public class GuestWeChatPaymentService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int QR_MINUTES = 30;

    private final JdbcTemplate jdbc;
    private final WeChatPayConfig.WeChatPayClient client;

    public GuestWeChatPaymentService(JdbcTemplate jdbc, WeChatPayConfig.WeChatPayClient client) {
        this.jdbc = jdbc;
        this.client = client;
    }

    @Transactional
    public WeChatNativePaymentService.Payment prepare(
            String orderNo, long guestAccessId, BigDecimal amount, String subject) {
        validate(amount, subject);
        List<OrderOwner> owners = jdbc.query("""
                SELECT business_no, customer_account_id, guest_access_id
                  FROM customer_orders
                 WHERE order_no = ? FOR UPDATE
                """, (rs, i) -> new OrderOwner(
                        rs.getString("business_no"),
                        (Long) rs.getObject("customer_account_id"),
                        (Long) rs.getObject("guest_access_id")), orderNo);
        if (owners.isEmpty()) {
            throw new IllegalArgumentException("订单不存在");
        }
        OrderOwner order = owners.getFirst();
        if (order.accountId() != null) {
            throw new ConflictException("该订单已经归属注册账号，请使用账号支付路径");
        }
        if (order.guestAccessId() == null || order.guestAccessId() != guestAccessId) {
            throw new ConflictException("游客业务凭证与订单不匹配");
        }

        Integer captured = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payment_order
                 WHERE business_no=? AND provider='WECHAT' AND status='CAPTURED'
                """, Integer.class, order.businessNo());
        if (captured != null && captured > 0) {
            throw new ConflictException("该订单已支付");
        }

        List<WeChatNativePaymentService.Payment> existing = jdbc.query("""
                SELECT payment_no,business_no,subject,amount,status,native_expires_at,native_code_url
                  FROM payment_order
                 WHERE business_no=? AND provider='WECHAT'
                   AND status IN ('CREATED','APPROVED','CAPTURED')
                 ORDER BY id DESC LIMIT 1
                """, this::payment, order.businessNo());
        if (!existing.isEmpty()) {
            WeChatNativePaymentService.Payment previous = existing.getFirst();
            if ("CAPTURED".equals(previous.status())) {
                throw new ConflictException("该订单已支付");
            }
            if (previous.amount().compareTo(amount) == 0
                    && previous.subject().equals(subject.trim())) {
                return previous;
            }
            throw new ConflictException("已有待支付单；改价前须先关闭该支付单");
        }

        String paymentNo = "XHP" + randomHex(12);
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO payment_order(
                  payment_no,user_id,guest_access_id,business_no,subject,amount,currency,
                  site_amount,site_currency,provider,status,idempotency_key,created_at,updated_at)
                VALUES (?,NULL,?,?,?,?, 'CNY',?,'CNY','WECHAT','CREATED',?,?,?)
                """, paymentNo, guestAccessId, order.businessNo(), subject.trim(), amount,
                amount, randomHex(16), timestamp(now), timestamp(now));
        return getForGuest(paymentNo, guestAccessId);
    }

    @Transactional(readOnly = true)
    public WeChatNativePaymentService.Payment latestForGuest(String businessNo, long guestAccessId) {
        List<WeChatNativePaymentService.Payment> rows = jdbc.query("""
                SELECT p.payment_no,p.business_no,p.subject,p.amount,p.status,
                       p.native_expires_at,p.native_code_url
                  FROM payment_order p
                  JOIN customer_orders o ON o.business_no=p.business_no
                 WHERE p.business_no=? AND p.guest_access_id=? AND o.guest_access_id=?
                   AND p.provider='WECHAT'
                 ORDER BY p.id DESC LIMIT 1
                """, this::payment, businessNo, guestAccessId, guestAccessId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Transactional(readOnly = true)
    public WeChatNativePaymentService.Payment getForGuest(String paymentNo, long guestAccessId) {
        List<WeChatNativePaymentService.Payment> rows = jdbc.query("""
                SELECT p.payment_no,p.business_no,p.subject,p.amount,p.status,
                       p.native_expires_at,p.native_code_url
                  FROM payment_order p
                  JOIN customer_orders o ON o.business_no=p.business_no
                 WHERE p.payment_no=? AND p.guest_access_id=? AND o.guest_access_id=?
                   AND p.provider='WECHAT'
                """, this::payment, paymentNo, guestAccessId, guestAccessId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("支付单不存在");
        }
        return rows.getFirst();
    }

    @Transactional
    public WeChatNativePaymentService.Payment nativeQr(String paymentNo, long guestAccessId) {
        List<WeChatNativePaymentService.Payment> rows = jdbc.query("""
                SELECT p.payment_no,p.business_no,p.subject,p.amount,p.status,
                       p.native_expires_at,p.native_code_url
                  FROM payment_order p
                  JOIN customer_orders o ON o.business_no=p.business_no
                 WHERE p.payment_no=? AND p.guest_access_id=? AND o.guest_access_id=?
                   AND p.provider='WECHAT'
                 FOR UPDATE
                """, this::payment, paymentNo, guestAccessId, guestAccessId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("支付单不存在");
        }
        WeChatNativePaymentService.Payment payment = rows.getFirst();
        if ("CAPTURED".equals(payment.status())) {
            return payment;
        }
        if ("CANCELLED".equals(payment.status())) {
            throw new ConflictException("支付单已关闭");
        }
        if ("APPROVED".equals(payment.status()) && payment.codeUrl() == null) {
            throw new ConflictException("支付单状态异常，请联系客服核对微信支付交易，暂勿重复付款");
        }
        if (payment.codeUrl() != null) {
            if (payment.expiresAt() != null && payment.expiresAt().isAfter(Instant.now())) {
                return payment;
            }
            throw new ConflictException("二维码已过期，请联系客服核对旧支付单后重新发起收款");
        }

        Instant expiry = NativePrepayTime.afterMinutes(Instant.now(), QR_MINUTES);
        PrepayRequest request = new PrepayRequest();
        Amount wechatAmount = new Amount();
        wechatAmount.setTotal(payment.amount().movePointRight(2).intValueExact());
        wechatAmount.setCurrency("CNY");
        request.setAmount(wechatAmount);
        request.setAppid(client.appId());
        request.setMchid(client.merchantId());
        request.setDescription(payment.subject());
        request.setOutTradeNo(payment.paymentNo());
        request.setNotifyUrl(client.notifyUrl());
        request.setTimeExpire(NativePrepayTime.timeExpire(expiry));
        String codeUrl = client.nativePay().prepay(request).getCodeUrl();
        if (codeUrl == null || !codeUrl.startsWith("weixin://")) {
            throw new ConflictException("微信支付未返回可用的扫码地址，请联系客服核对商户配置");
        }
        jdbc.update("""
                UPDATE payment_order
                   SET status='APPROVED',native_code_url=?,native_expires_at=?,updated_at=?
                 WHERE payment_no=? AND status='CREATED'
                """, codeUrl, timestamp(expiry), timestamp(Instant.now()), paymentNo);
        return getForGuest(paymentNo, guestAccessId);
    }

    private void validate(BigDecimal amount, String subject) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.compareTo(new BigDecimal("9999999.99")) > 0) {
            throw new IllegalArgumentException("收款金额必须为大于零的人民币金额，最多两位小数");
        }
        if (subject == null || subject.isBlank()
                || subject.trim().getBytes(StandardCharsets.UTF_8).length > 127) {
            throw new IllegalArgumentException("请填写不超过 127 字节的服务名称");
        }
    }

    private WeChatNativePaymentService.Payment payment(ResultSet rs, int rowNum) throws SQLException {
        Timestamp expires = rs.getTimestamp("native_expires_at");
        return new WeChatNativePaymentService.Payment(
                rs.getString("payment_no"),
                rs.getString("business_no"),
                rs.getString("subject"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                expires == null ? null : expires.toInstant(),
                rs.getString("native_code_url"));
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return HexFormat.of().formatHex(value);
    }

    private record OrderOwner(String businessNo, Long accountId, Long guestAccessId) {}
}
