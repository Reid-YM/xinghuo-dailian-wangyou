package com.xinghuo.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Final amounts are set by staff; the browser never supplies a charge amount. */
@Service
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "enabled", havingValue = "true")
public class WeChatNativePaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(WeChatNativePaymentService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int QR_MINUTES = 30;
    private final JdbcTemplate jdbc;
    private final WeChatPayConfig.WeChatPayClient client;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    public WeChatNativePaymentService(JdbcTemplate jdbc, WeChatPayConfig.WeChatPayClient client,
                                      ObjectMapper json, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.client = client;
        this.json = json;
        this.transactions = transactions;
    }

    public record Payment(String paymentNo, String businessNo, String subject,
                          BigDecimal amount, String status, Instant expiresAt,
                          String codeUrl) {}

    @Transactional
    public Payment prepare(String orderNo, BigDecimal amount, String subject) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.compareTo(new BigDecimal("9999999.99")) > 0) {
            throw new IllegalArgumentException("收款金额必须为大于零的人民币金额，最多两位小数");
        }
        if (subject == null || subject.isBlank()
                || subject.trim().getBytes(StandardCharsets.UTF_8).length > 127) {
            throw new IllegalArgumentException("请填写不超过 127 字节的服务名称");
        }
        List<OrderOwner> orders = jdbc.query("""
                SELECT business_no, customer_account_id FROM customer_orders
                 WHERE order_no = ? FOR UPDATE
                """, (rs, i) -> new OrderOwner(rs.getString(1), (Long) rs.getObject(2)), orderNo);
        if (orders.isEmpty()) throw new IllegalArgumentException("订单不存在");
        OrderOwner order = orders.getFirst();
        if (order.accountId() == null) {
            throw new IllegalStateException("订单尚未归属客户账号，请先完成账号关联");
        }
        List<Payment> existing = jdbc.query("""
                SELECT payment_no,business_no,subject,amount,status,native_expires_at,native_code_url
                  FROM payment_order WHERE business_no = ? AND provider = 'WECHAT'
                   AND status IN ('CREATED','APPROVED','CAPTURED') ORDER BY id DESC LIMIT 1
                """, PAYMENT_MAPPER, order.businessNo());
        Integer captured = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payment_order WHERE business_no=?
                  AND provider='WECHAT' AND status='CAPTURED'
                """, Integer.class, order.businessNo());
        if (captured != null && captured > 0) throw new IllegalStateException("该订单已支付");
        if (!existing.isEmpty()) {
            Payment previous = existing.getFirst();
            if (previous.status().equals("CAPTURED")) throw new IllegalStateException("该订单已支付");
            if (previous.amount().compareTo(amount) == 0 && previous.subject().equals(subject.trim())) {
                return previous;
            }
            throw new IllegalStateException("已有待支付单；改价前须先关闭该支付单");
        }
        String paymentNo = "XHP" + randomHex(12);
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO payment_order(payment_no,user_id,business_no,subject,amount,currency,
                  site_amount,site_currency,provider,status,idempotency_key,created_at,updated_at)
                VALUES (?,?,?,?,?,'CNY',?,'CNY','WECHAT','CREATED',?,?,?)
                """, paymentNo, order.accountId(), order.businessNo(), subject.trim(), amount,
                amount, randomHex(16), utc(now), utc(now));
        return getForAccount(paymentNo, order.accountId());
    }

    @Transactional(readOnly = true)
    public Payment latestForOrder(String businessNo, long accountId) {
        List<Payment> rows = jdbc.query("""
                SELECT p.payment_no,p.business_no,p.subject,p.amount,p.status,p.native_expires_at,p.native_code_url
                  FROM payment_order p JOIN customer_orders o ON o.business_no=p.business_no
                 WHERE p.business_no = ? AND p.user_id = ? AND o.customer_account_id = ?
                   AND p.provider = 'WECHAT'
                 ORDER BY p.id DESC LIMIT 1
                """, PAYMENT_MAPPER, businessNo, accountId, accountId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Transactional(readOnly = true)
    public Payment latestForAdminOrder(String orderNo) {
        List<Payment> rows = jdbc.query("""
                SELECT p.payment_no,p.business_no,p.subject,p.amount,p.status,p.native_expires_at,p.native_code_url
                  FROM payment_order p JOIN customer_orders o ON o.business_no=p.business_no
                 WHERE o.order_no=? AND p.provider='WECHAT'
                 ORDER BY p.id DESC LIMIT 1
                """, PAYMENT_MAPPER, orderNo);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Transactional(readOnly = true)
    public Payment getForAccount(String paymentNo, long accountId) {
        List<Payment> rows = jdbc.query("""
                SELECT p.payment_no,p.business_no,p.subject,p.amount,p.status,p.native_expires_at,p.native_code_url
                  FROM payment_order p JOIN customer_orders o ON o.business_no=p.business_no
                 WHERE p.payment_no = ? AND p.user_id = ? AND o.customer_account_id = ?
                   AND p.provider = 'WECHAT'
                """, PAYMENT_MAPPER, paymentNo, accountId, accountId);
        if (rows.isEmpty()) throw new IllegalArgumentException("支付单不存在");
        return rows.getFirst();
    }

    @Transactional
    public Payment nativeQr(String paymentNo, long accountId) {
        Payment payment = lockForAccount(paymentNo, accountId);
        if (payment.status().equals("CAPTURED")) return payment;
        if (payment.status().equals("CANCELLED")) throw new IllegalStateException("支付单已关闭");
        // Never issue or reuse a Native QR for refunded, failed, or unknown states.
        // APPROVED without a QR is inconsistent; retrying prepay with the same
        // out_trade_no could create a remote intent that cannot be persisted.
        if (!"CREATED".equals(payment.status()) && !"APPROVED".equals(payment.status())) {
            throw new IllegalStateException("支付单状态不允许生成二维码，请联系客服核对收款记录");
        }
        if ("APPROVED".equals(payment.status()) && payment.codeUrl() == null) {
            throw new IllegalStateException("支付单状态异常，请联系客服核对微信支付交易，暂勿重复付款");
        }
        if (payment.codeUrl() != null) {
            if (payment.expiresAt() != null && payment.expiresAt().isAfter(Instant.now())) return payment;
            throw new IllegalStateException("二维码已过期，请联系客服重新确认支付单");
        }
        // External request runs under the row lock so concurrent clicks cannot issue two codes.
        Instant expiry = NativePrepayTime.afterMinutes(Instant.now(), QR_MINUTES);
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(payment.amount().movePointRight(2).intValueExact());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        request.setAppid(client.appId());
        request.setMchid(client.merchantId());
        request.setDescription(payment.subject());
        request.setOutTradeNo(payment.paymentNo());
        request.setNotifyUrl(client.notifyUrl());
        request.setTimeExpire(NativePrepayTime.timeExpire(expiry));
        String codeUrl = client.nativePay().prepay(request).getCodeUrl();
        if (codeUrl == null || !codeUrl.startsWith("weixin://")) {
            throw new IllegalStateException("微信支付未返回可用的扫码地址");
        }
        jdbc.update("""
                UPDATE payment_order SET status='APPROVED',native_code_url=?,native_expires_at=?,
                 updated_at=? WHERE payment_no=? AND status='CREATED'
                """, codeUrl, utc(expiry), utc(Instant.now()), paymentNo);
        return getForAccount(paymentNo, accountId);
    }

    @Transactional
    public void acceptNotification(RequestParam request, String rawBody) throws Exception {
        if (rawBody == null || rawBody.getBytes(StandardCharsets.UTF_8).length > 65536) {
            throw new IllegalArgumentException("通知报文超出限制");
        }
        // SDK verifies the original bytes before decryption; never trust JSON parsed before this call.
        Transaction transaction = client.parser().parse(request, Transaction.class);
        JsonNode envelope = json.readTree(rawBody);
        String eventId = envelope.path("id").asText("");
        if (eventId.isBlank() || eventId.length() > 80) throw new IllegalArgumentException("通知编号无效");
        if (transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS) return;
        int inserted = jdbc.update("""
                INSERT IGNORE INTO payment_webhook_event(provider,event_id,event_type,payment_no,
                  signature_verified,raw_body,process_result,received_at)
                VALUES ('WECHAT',?,?,?,1,?,'PROCESSING',?)
                """, eventId, envelope.path("event_type").asText(""), transaction.getOutTradeNo(),
                rawBody, utc(Instant.now()));
        if (inserted == 0) return;
        applySuccess(transaction);
        jdbc.update("""
                UPDATE payment_webhook_event SET process_result='SUCCESS',processed_at=?
                 WHERE provider='WECHAT' AND event_id=?
                """, utc(Instant.now()), eventId);
    }

    /** SDK query is the recovery path when the callback is delayed or lost. */
    public void reconcile(String paymentNo) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(client.merchantId());
        request.setOutTradeNo(paymentNo);
        Transaction transaction;
        try {
            transaction = client.nativePay().queryOrderByOutTradeNo(request);
        } catch (ServiceException error) {
            if ("ORDER_NOT_EXISTS".equals(error.getErrorCode())) {
                jdbc.update("UPDATE payment_order SET updated_at=? WHERE payment_no=? AND status='APPROVED'",
                        utc(Instant.now()), paymentNo);
                return;
            }
            throw error;
        }
        if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
            transactions.executeWithoutResult(status -> applySuccess(transaction));
        } else if (transaction.getTradeState() == Transaction.TradeStateEnum.CLOSED) {
            jdbc.update("UPDATE payment_order SET status='CANCELLED',updated_at=? "
                    + "WHERE payment_no=? AND status IN ('CREATED','APPROVED')",
                    utc(Instant.now()), paymentNo);
        } else {
            jdbc.update("UPDATE payment_order SET updated_at=? WHERE payment_no=? AND status='APPROVED'",
                    utc(Instant.now()), paymentNo);
        }
    }

    @Transactional
    public boolean close(String paymentNo) {
        List<String> statuses = jdbc.query("SELECT status FROM payment_order WHERE payment_no=? "
                + "AND provider='WECHAT' FOR UPDATE", (rs, i) -> rs.getString(1), paymentNo);
        if (statuses.isEmpty()) throw new IllegalArgumentException("支付单不存在");
        if (statuses.getFirst().equals("CAPTURED")) throw new IllegalStateException("已支付的订单不能关闭");
        if (statuses.getFirst().equals("CANCELLED")) return true;
        QueryOrderByOutTradeNoRequest query = new QueryOrderByOutTradeNoRequest();
        query.setMchid(client.merchantId());
        query.setOutTradeNo(paymentNo);
        try {
            Transaction transaction = client.nativePay().queryOrderByOutTradeNo(query);
            if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                applySuccess(transaction);
                return false;
            }
        } catch (ServiceException error) {
            if (!"ORDER_NOT_EXISTS".equals(error.getErrorCode())) throw error;
        }
        CloseOrderRequest close = new CloseOrderRequest();
        close.setMchid(client.merchantId());
        close.setOutTradeNo(paymentNo);
        try {
            client.nativePay().closeOrder(close);
        } catch (ServiceException error) {
            if (!"ORDER_NOT_EXISTS".equals(error.getErrorCode())) throw error;
        }
        jdbc.update("UPDATE payment_order SET status='CANCELLED',updated_at=? "
                + "WHERE payment_no=? AND status IN ('CREATED','APPROVED')",
                utc(Instant.now()), paymentNo);
        return true;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void recoverPending() {
        List<Pending> pending = jdbc.query("""
                SELECT payment_no,native_expires_at FROM payment_order
                 WHERE provider='WECHAT' AND status='APPROVED'
                 ORDER BY updated_at ASC LIMIT 30
                """, (rs, i) -> new Pending(rs.getString(1), asInstant(rs.getObject(2, LocalDateTime.class))));
        for (Pending row : pending) {
            try {
                reconcile(row.paymentNo());
                if (row.expiresAt() != null && row.expiresAt().isBefore(Instant.now())) {
                    close(row.paymentNo());
                }
            } catch (RuntimeException error) {
                // Keep the payment pending so the next run retries; never infer success from a timeout.
                LOG.warn("WeChat Pay reconciliation failed for {}: {}", row.paymentNo(), error.getClass().getSimpleName());
            }
        }
    }

    private void applySuccess(Transaction transaction) {
        if (!Objects.equals(client.merchantId(), transaction.getMchid())
                || !Objects.equals(client.appId(), transaction.getAppid())
                || transaction.getOutTradeNo() == null || transaction.getAmount() == null
                || transaction.getAmount().getTotal() == null
                || transaction.getTransactionId() == null || transaction.getTransactionId().isBlank()) {
            throw new IllegalArgumentException("微信支付通知商户信息不匹配");
        }
        List<PaymentWithId> rows = jdbc.query("""
                SELECT id,amount,status,provider_order_id FROM payment_order
                 WHERE payment_no=? AND provider='WECHAT' FOR UPDATE
                """, (rs, i) -> new PaymentWithId(rs.getLong(1), rs.getBigDecimal(2), rs.getString(3), rs.getString(4)),
                transaction.getOutTradeNo());
        if (rows.isEmpty()) throw new IllegalArgumentException("微信支付通知订单不存在");
        PaymentWithId row = rows.getFirst();
        if (row.amount().movePointRight(2).intValueExact() != transaction.getAmount().getTotal()
                || !"CNY".equals(transaction.getAmount().getCurrency())) {
            throw new IllegalArgumentException("微信支付通知金额不匹配");
        }
        // A repeated success reports the original receipt, not a reversal of a
        // later refund. Keep both settled states idempotent under the row lock.
        if (row.status().equals("CAPTURED") || row.status().equals("REFUNDED")) {
            if (!transaction.getTransactionId().equals(row.providerOrderId())) {
                throw new IllegalStateException("已收款或退款的支付单收到不匹配的微信流水号，请人工核查");
            }
            return;
        }
        if (row.status().equals("CANCELLED")) {
            LOG.error("Closed WeChat Pay order {} received verified success; recording receipt for reconciliation",
                    transaction.getOutTradeNo());
        }
        Instant now = Instant.now();
        jdbc.update("""
                UPDATE payment_order SET status='CAPTURED',provider_order_id=?,
                  provider_capture_id=?,paid_at=?,updated_at=? WHERE id=?
                """, transaction.getTransactionId(), transaction.getTransactionId(),
                utc(now), utc(now), row.id());
        jdbc.update("""
                INSERT INTO payment_transaction(payment_id,kind,provider_txn_id,amount,currency,status,created_at)
                VALUES (?,'CAPTURE',?,?,'CNY','SUCCESS',?)
                """, row.id(), transaction.getTransactionId(), row.amount(), utc(now));
    }

    private Payment lockForAccount(String paymentNo, long accountId) {
        List<Payment> rows = jdbc.query("""
                SELECT p.payment_no,p.business_no,p.subject,p.amount,p.status,p.native_expires_at,p.native_code_url
                  FROM payment_order p JOIN customer_orders o ON o.business_no=p.business_no
                 WHERE p.payment_no=? AND p.user_id=? AND o.customer_account_id=?
                   AND p.provider='WECHAT' FOR UPDATE
                """, PAYMENT_MAPPER, paymentNo, accountId, accountId);
        if (rows.isEmpty()) throw new IllegalArgumentException("支付单不存在");
        return rows.getFirst();
    }

    private static final org.springframework.jdbc.core.RowMapper<Payment> PAYMENT_MAPPER =
            (rs, i) -> new Payment(rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getBigDecimal(4), rs.getString(5), asInstant(rs.getObject(6, LocalDateTime.class)), rs.getString(7));

    private static Instant asInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return HexFormat.of().formatHex(value).toUpperCase();
    }

    private record OrderOwner(String businessNo, Long accountId) {}
    private record PaymentWithId(long id, BigDecimal amount, String status, String providerOrderId) {}
    private record Pending(String paymentNo, Instant expiresAt) {}
}
