package com.xinghuo.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class WeChatNativePaymentServiceTest {
    private JdbcTemplate jdbc;
    private NotificationParser parser;
    private NativePayService nativePay;
    private WeChatNativePaymentService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:pay_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE payment_order(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,payment_no VARCHAR(32) UNIQUE,
                  user_id BIGINT,business_no VARCHAR(40),subject VARCHAR(200),
                  amount DECIMAL(12,2),currency VARCHAR(8),site_amount DECIMAL(12,2),
                  site_currency VARCHAR(8),status VARCHAR(20),provider VARCHAR(16),
                  idempotency_key VARCHAR(64),created_at TIMESTAMP,
                  native_code_url VARCHAR(512),native_expires_at TIMESTAMP,
                  provider_order_id VARCHAR(64),provider_capture_id VARCHAR(64),
                  paid_at TIMESTAMP,updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE customer_orders(order_no VARCHAR(40) PRIMARY KEY,
                  business_no VARCHAR(40) UNIQUE,customer_account_id BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE payment_transaction(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,payment_id BIGINT,kind VARCHAR(16),
                  provider_txn_id VARCHAR(64) UNIQUE,amount DECIMAL(12,2),currency VARCHAR(8),
                  status VARCHAR(20),created_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE payment_webhook_event(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,provider VARCHAR(16),event_id VARCHAR(80),
                  event_type VARCHAR(80),payment_no VARCHAR(32),signature_verified BOOLEAN,
                  raw_body CLOB,process_result VARCHAR(200),received_at TIMESTAMP,
                  processed_at TIMESTAMP,UNIQUE(provider,event_id))
                """);
        jdbc.update("INSERT INTO customer_orders(order_no,business_no,customer_account_id) VALUES ('O1','B1',7)");
        jdbc.update("INSERT INTO customer_orders(order_no,business_no,customer_account_id) VALUES ('O2','B2',7)");
        jdbc.update("""
                INSERT INTO payment_order(payment_no,user_id,business_no,subject,amount,status,provider)
                VALUES (?,?,?,?,?,?,?)
                """, "XHPTEST01", 7, "B1", "测试服务", new BigDecimal("12.34"), "APPROVED", "WECHAT");
        parser = mock(NotificationParser.class);
        nativePay = mock(NativePayService.class);
        service = new WeChatNativePaymentService(jdbc,
                new WeChatPayConfig.WeChatPayClient("1118501429", "wxcc2af059160a52da", "https://example.test/api/wechat-pay/notify",
                        nativePay, parser),
                new ObjectMapper(), new TransactionTemplate(new DataSourceTransactionManager(dataSource)));
    }

    @Test
    void finalAmountComesFromStaffAndQrIsRestrictedToOwningAccount() {
        WeChatNativePaymentService.Payment payment = service.prepare("O2", new BigDecimal("29.80"), "代练服务");
        assertThrows(IllegalArgumentException.class, () -> service.getForAccount(payment.paymentNo(), 8));
        assertThrows(IllegalArgumentException.class, () -> service.nativeQr(payment.paymentNo(), 8));
        PrepayResponse response = new PrepayResponse();
        response.setCodeUrl("weixin://wxpay/bizpayurl?pr=test");
        when(nativePay.prepay(any(PrepayRequest.class))).thenReturn(response);
        service.nativeQr(payment.paymentNo(), 7);
        service.nativeQr(payment.paymentNo(), 7);
        ArgumentCaptor<PrepayRequest> request = ArgumentCaptor.forClass(PrepayRequest.class);
        verify(nativePay, times(1)).prepay(request.capture());
        assertEquals(2980, request.getValue().getAmount().getTotal());
        assertEquals(payment.paymentNo(), request.getValue().getOutTradeNo());
        // Verify the actual SDK request, not only the helper formatter.
        String gatewayExpiry = request.getValue().getTimeExpire();
        org.junit.jupiter.api.Assertions.assertTrue(gatewayExpiry.matches(
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+08:00"));
        assertEquals(service.getForAccount(payment.paymentNo(), 7).expiresAt(),
                java.time.OffsetDateTime.parse(gatewayExpiry).toInstant());
        assertEquals("APPROVED", service.getForAccount(payment.paymentNo(), 7).status());
    }

    @Test
    void qrNeverReusesRefundedOrFailedPaymentEvenWhenOldCodeLooksValid() {
        String validLookingQr = "weixin://wxpay/old-receipt-must-not-be-reused";
        for (String status : new String[]{"REFUNDED", "FAILED", "UNKNOWN"}) {
            jdbc.update("""
                    UPDATE payment_order
                       SET status=?,native_code_url=?,native_expires_at=?
                     WHERE payment_no='XHPTEST01'
                    """, status, validLookingQr,
                    java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(600)));
            assertThrows(IllegalStateException.class, () -> service.nativeQr("XHPTEST01", 7),
                    "Must refuse a terminal or unknown payment state: " + status);
        }
        org.mockito.Mockito.verifyNoInteractions(nativePay);
        assertEquals("UNKNOWN", jdbc.queryForObject(
                "SELECT status FROM payment_order WHERE payment_no='XHPTEST01'", String.class));
    }

    @Test
    void approvedPaymentWithoutPersistedQrCannotIssueAnotherRemotePrepay() {
        // An APPROVED row without code URL may represent an incomplete external
        // operation. Never call the WeChat prepay API with that stale payment id.
        assertThrows(IllegalStateException.class, () -> service.nativeQr("XHPTEST01", 7));
        org.mockito.Mockito.verifyNoInteractions(nativePay);
    }

    @Test
    void verifiedDuplicateNotificationCreditsExactlyOnce() throws Exception {
        when(parser.parse(any(RequestParam.class), eq(Transaction.class))).thenReturn(success(1234));
        String body = "{\"id\":\"evt-1\",\"event_type\":\"TRANSACTION.SUCCESS\"}";
        service.acceptNotification(request(body), body);
        service.acceptNotification(request(body), body);
        assertEquals("CAPTURED", jdbc.queryForObject(
                "SELECT status FROM payment_order WHERE payment_no='XHPTEST01'", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payment_webhook_event", Integer.class));
    }

    @Test
    void wrongAmountNeverCreditsPayment() throws Exception {
        when(parser.parse(any(RequestParam.class), eq(Transaction.class))).thenReturn(success(1235));
        String body = "{\"id\":\"evt-2\",\"event_type\":\"TRANSACTION.SUCCESS\"}";
        assertThrows(IllegalArgumentException.class, () -> service.acceptNotification(request(body), body));
        assertEquals("APPROVED", jdbc.queryForObject(
                "SELECT status FROM payment_order WHERE payment_no='XHPTEST01'", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void wrongMerchantNeverCreditsPayment() throws Exception {
        Transaction transaction = success(1234);
        transaction.setMchid("other-merchant");
        when(parser.parse(any(RequestParam.class), eq(Transaction.class))).thenReturn(transaction);
        String body = "{\"id\":\"evt-3\",\"event_type\":\"TRANSACTION.SUCCESS\"}";
        assertThrows(IllegalArgumentException.class, () -> service.acceptNotification(request(body), body));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    private Transaction success(int cents) {
        Transaction transaction = new Transaction();
        transaction.setMchid("1118501429");
        transaction.setAppid("wxcc2af059160a52da");
        transaction.setOutTradeNo("XHPTEST01");
        transaction.setTransactionId("420000TEST01");
        transaction.setTradeState(Transaction.TradeStateEnum.SUCCESS);
        TransactionAmount amount = new TransactionAmount();
        amount.setTotal(cents);
        amount.setCurrency("CNY");
        transaction.setAmount(amount);
        return transaction;
    }

    private RequestParam request(String body) {
        return new RequestParam.Builder().serialNumber("PUB_KEY_TEST")
                .nonce("nonce").signature("signature").timestamp("1234567890").body(body).build();
    }
}
