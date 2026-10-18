package com.xinghuo.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** The anonymous checkout path must send the same valid Native request as account checkout. */
class GuestWeChatNativePrepayTest {
    @Test
    void guestNativeQrDoesNotSendNanosecondExpiryToWeChat() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:guest_native_" + UUID.randomUUID()
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
                CREATE TABLE customer_orders (
                  order_no VARCHAR(64) PRIMARY KEY,business_no VARCHAR(40) UNIQUE,
                  customer_account_id BIGINT,guest_access_id BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE payment_order (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,payment_no VARCHAR(32) UNIQUE,
                  user_id BIGINT,guest_access_id BIGINT,business_no VARCHAR(40),
                  subject VARCHAR(127),amount DECIMAL(10,2),currency VARCHAR(8),
                  site_amount DECIMAL(10,2),site_currency VARCHAR(8),
                  provider VARCHAR(16),status VARCHAR(24),idempotency_key VARCHAR(64),
                  created_at TIMESTAMP,updated_at TIMESTAMP,native_expires_at TIMESTAMP,
                  native_code_url VARCHAR(512))
                """);
        jdbc.update("""
                INSERT INTO customer_orders(order_no,business_no,customer_account_id,guest_access_id)
                VALUES ('O-GUEST','B-GUEST',NULL,21)
                """);
        NativePayService api = mock(NativePayService.class);
        var client = new WeChatPayConfig.WeChatPayClient(
                "1234567890", "wx1234567890abcdef", "https://example.com/api/wechat-pay/notify",
                api, null);
        var service = new GuestWeChatPaymentService(jdbc, client);
        var payment = service.prepare("O-GUEST", 21, new BigDecimal("10.23"), "MW4 代练服务");

        PrepayResponse prepay = new PrepayResponse();
        prepay.setCodeUrl("weixin://wxpay/bizpayurl/up?pr=test");
        when(api.prepay(any(PrepayRequest.class))).thenReturn(prepay);
        var first = service.nativeQr(payment.paymentNo(), 21);
        var second = service.nativeQr(payment.paymentNo(), 21);

        ArgumentCaptor<PrepayRequest> sent = ArgumentCaptor.forClass(PrepayRequest.class);
        verify(api, times(1)).prepay(sent.capture());
        PrepayRequest request = sent.getValue();
        assertEquals(1023, request.getAmount().getTotal());
        assertEquals("B-GUEST", first.businessNo());
        assertEquals("APPROVED", first.status());
        assertEquals(first.paymentNo(), second.paymentNo());
        String expire = request.getTimeExpire();
        assertTrue(expire.matches(
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+08:00"));
        assertEquals(first.expiresAt(), OffsetDateTime.parse(expire).toInstant());

        for (String state : new String[]{"REFUNDED", "FAILED", "UNKNOWN"}) {
            jdbc.update("""
                    UPDATE payment_order SET status=?,native_code_url=?,
                           native_expires_at=? WHERE payment_no=?
                    """, state, "weixin://wxpay/old-refunded-qr",
                    java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(600)),
                    payment.paymentNo());
            assertThrows(com.xinghuo.exception.ConflictException.class,
                    () -> service.nativeQr(payment.paymentNo(), 21));
        }
        // Even an APPROVED payment may have had an incomplete remote operation.
        // A null QR must be investigated, not used for a second remote prepay.
        jdbc.update("UPDATE payment_order SET status='APPROVED',native_code_url=NULL WHERE payment_no=?",
                payment.paymentNo());
        assertThrows(com.xinghuo.exception.ConflictException.class,
                () -> service.nativeQr(payment.paymentNo(), 21));
        verify(api, times(1)).prepay(any(PrepayRequest.class));
    }
}
