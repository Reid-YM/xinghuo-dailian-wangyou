package com.xinghuo.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    void newSuccessEventAfterRefundPreservesRefundAndOriginalReceipt() throws Exception {
        when(parser.parse(any(RequestParam.class), eq(Transaction.class))).thenReturn(success(1234));
        String original = "{\"id\":\"original-receipt\",\"event_type\":\"TRANSACTION.SUCCESS\"}";
        service.acceptNotification(request(original), original);
        jdbc.update("UPDATE payment_order SET status='REFUNDED' WHERE payment_no='XHPTEST01'");
        String replay = "{\"id\":\"replayed-receipt\",\"event_type\":\"TRANSACTION.SUCCESS\"}";

        service.acceptNotification(request(replay), replay);
        service.acceptNotification(request(replay), replay);

        assertEquals("REFUNDED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_webhook_event WHERE process_result='SUCCESS'", Integer.class));
        assertEquals("420000TEST01", jdbc.queryForObject(
                "SELECT provider_order_id FROM payment_order WHERE payment_no='XHPTEST01'", String.class));
    }

    @Test
    void reconciliationAfterRefundDoesNotRestoreCapturedStateOrDuplicateReceipt() {
        jdbc.update("""
                UPDATE payment_order SET status='REFUNDED',provider_order_id='420000TEST01'
                 WHERE payment_no='XHPTEST01'
                """);
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(success(1234));

        service.reconcile("XHPTEST01");
        service.reconcile("XHPTEST01");

        assertEquals("REFUNDED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void refundedPaymentWithDifferentOrMissingOriginalReceiptRequiresReview() {
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(success(1234));
        for (String originalId : new String[]{"different-receipt", null}) {
            jdbc.update("""
                    UPDATE payment_order SET status='REFUNDED',provider_order_id=?
                     WHERE payment_no='XHPTEST01'
                    """, originalId);

            assertThrows(IllegalStateException.class, () -> service.reconcile("XHPTEST01"));

            assertEquals("REFUNDED", service.getForAccount("XHPTEST01", 7).status());
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        }
    }

    @Test
    void refundedPaymentStillValidatesReceiptAmountBeforeIgnoringReplay() {
        jdbc.update("""
                UPDATE payment_order SET status='REFUNDED',provider_order_id='420000TEST01'
                 WHERE payment_no='XHPTEST01'
                """);
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(success(1235));

        assertThrows(IllegalArgumentException.class, () -> service.reconcile("XHPTEST01"));

        assertEquals("REFUNDED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void verifiedLateSuccessForCancelledPaymentStillRecordsActualReceipt() {
        jdbc.update("UPDATE payment_order SET status='CANCELLED' WHERE payment_no='XHPTEST01'");
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(success(1234));

        service.reconcile("XHPTEST01");
        service.reconcile("XHPTEST01");

        assertEquals("CAPTURED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void closeRejectsRefundedFailedAndUnknownStatesWithoutCallingWeChat() {
        for (String status : new String[]{"REFUNDED", "FAILED", "UNKNOWN"}) {
            jdbc.update("UPDATE payment_order SET status=? WHERE payment_no='XHPTEST01'", status);

            assertThrows(IllegalStateException.class, () -> service.close("XHPTEST01"));

            assertEquals(status, service.getForAccount("XHPTEST01", 7).status());
        }
        org.mockito.Mockito.verifyNoInteractions(nativePay);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void closeIsIdempotentForCancelledAndRejectsCapturedWithoutGatewayCalls() {
        jdbc.update("UPDATE payment_order SET status='CANCELLED' WHERE payment_no='XHPTEST01'");
        assertTrue(service.close("XHPTEST01"));
        assertTrue(service.close("XHPTEST01"));
        assertEquals("CANCELLED", service.getForAccount("XHPTEST01", 7).status());

        jdbc.update("UPDATE payment_order SET status='CAPTURED' WHERE payment_no='XHPTEST01'");
        assertThrows(IllegalStateException.class, () -> service.close("XHPTEST01"));
        assertEquals("CAPTURED", service.getForAccount("XHPTEST01", 7).status());
        org.mockito.Mockito.verifyNoInteractions(nativePay);
    }

    @Test
    void pendingPaymentClosesOnlyAfterGatewayConfirmsClose() {
        Transaction unpaid = success(1234);
        unpaid.setTradeState(Transaction.TradeStateEnum.NOTPAY);
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class))).thenReturn(unpaid);
        for (String status : new String[]{"CREATED", "APPROVED"}) {
            jdbc.update("UPDATE payment_order SET status=? WHERE payment_no='XHPTEST01'", status);

            assertTrue(service.close("XHPTEST01"));

            assertEquals("CANCELLED", service.getForAccount("XHPTEST01", 7).status());
        }
        verify(nativePay, times(2)).closeOrder(any(CloseOrderRequest.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void closeRecordsDiscoveredPaymentInsteadOfClosingIt() {
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(success(1234));

        assertFalse(service.close("XHPTEST01"));

        assertEquals("CAPTURED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        verify(nativePay, org.mockito.Mockito.never()).closeOrder(any(CloseOrderRequest.class));
    }

    @Test
    void closeGatewayFailureKeepsPaymentPendingForVerification() {
        Transaction unpaid = success(1234);
        unpaid.setTradeState(Transaction.TradeStateEnum.NOTPAY);
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class))).thenReturn(unpaid);
        org.mockito.Mockito.doThrow(new IllegalStateException("gateway unavailable"))
                .when(nativePay).closeOrder(any(CloseOrderRequest.class));

        assertThrows(IllegalStateException.class, () -> service.close("XHPTEST01"));

        assertEquals("APPROVED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void scheduledExpiredPaymentClosesUnderAnActualTransaction() {
        expirePending("XHPTEST01");
        Transaction unpaid = success(1234);
        unpaid.setTradeState(Transaction.TradeStateEnum.NOTPAY);
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class))).thenReturn(unpaid);
        org.mockito.Mockito.doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(nativePay).closeOrder(any(CloseOrderRequest.class));

        service.recoverPending();

        assertEquals("CANCELLED", service.getForAccount("XHPTEST01", 7).status());
        verify(nativePay).closeOrder(any(CloseOrderRequest.class));
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    }

    @Test
    void scheduledCloseRollsBackReceiptStateWhenLedgerWriteFails() {
        expirePending("XHPTEST01");
        jdbc.update("""
                INSERT INTO payment_transaction(payment_id,kind,provider_txn_id,amount,currency,status)
                VALUES (99,'CAPTURE','420000TEST01',12.34,'CNY','SUCCESS')
                """);
        Transaction unpaid = success(1234);
        unpaid.setTradeState(Transaction.TradeStateEnum.NOTPAY);
        // First query is still unpaid; a payment arrives just before close's second query.
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(unpaid, success(1234));

        service.recoverPending();

        assertEquals("APPROVED", service.getForAccount("XHPTEST01", 7).status());
        org.junit.jupiter.api.Assertions.assertNull(jdbc.queryForObject(
                "SELECT provider_order_id FROM payment_order WHERE payment_no='XHPTEST01'", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        verify(nativePay, times(2)).queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class));
        verify(nativePay, org.mockito.Mockito.never()).closeOrder(any(CloseOrderRequest.class));
    }

    @Test
    void scheduledCloseFailureDoesNotStopTheNextExpiredPayment() {
        jdbc.update("""
                INSERT INTO payment_order(payment_no,user_id,business_no,subject,amount,status,provider)
                VALUES ('XHPTEST02',7,'B2','测试服务',12.34,'APPROVED','WECHAT')
                """);
        expirePending("XHPTEST01");
        expirePending("XHPTEST02");
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenAnswer(call -> {
                    QueryOrderByOutTradeNoRequest query = call.getArgument(0);
                    Transaction unpaid = success(1234);
                    unpaid.setOutTradeNo(query.getOutTradeNo());
                    unpaid.setTradeState(Transaction.TradeStateEnum.NOTPAY);
                    return unpaid;
                });
        org.mockito.Mockito.doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            CloseOrderRequest request = call.getArgument(0);
            if ("XHPTEST01".equals(request.getOutTradeNo())) {
                throw new IllegalStateException("gateway unavailable");
            }
            return null;
        }).when(nativePay).closeOrder(any(CloseOrderRequest.class));

        service.recoverPending();

        assertEquals("APPROVED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals("CANCELLED", service.getForAccount("XHPTEST02", 7).status());
        verify(nativePay, times(2)).closeOrder(any(CloseOrderRequest.class));
    }

    @Test
    void queryForAnotherOrderCannotChangeEitherLocalPayment() {
        jdbc.update("""
                INSERT INTO payment_order(payment_no,user_id,business_no,subject,amount,status,provider)
                VALUES ('XHPTEST02',7,'B2','测试服务',12.34,'APPROVED','WECHAT')
                """);
        for (Transaction.TradeStateEnum state : new Transaction.TradeStateEnum[]{
                Transaction.TradeStateEnum.SUCCESS, Transaction.TradeStateEnum.CLOSED}) {
            Transaction other = success(1234);
            other.setOutTradeNo("XHPTEST02");
            other.setTradeState(state);
            when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                    .thenReturn(other);

            assertThrows(IllegalStateException.class, () -> service.reconcile("XHPTEST01"));
            assertThrows(IllegalStateException.class, () -> service.close("XHPTEST01"));

            assertEquals("APPROVED", service.getForAccount("XHPTEST01", 7).status());
            assertEquals("APPROVED", service.getForAccount("XHPTEST02", 7).status());
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        }
        verify(nativePay, org.mockito.Mockito.never()).closeOrder(any(CloseOrderRequest.class));
    }

    @Test
    void incompleteOrMismatchedQueryIdentityNeverCancelsPayment() {
        for (String fault : new String[]{"merchant", "application", "missing-order",
                "missing-merchant", "missing-application", "missing-state", "empty"}) {
            Transaction result = success(1234);
            result.setTradeState(Transaction.TradeStateEnum.CLOSED);
            switch (fault) {
                case "merchant" -> result.setMchid("other-merchant");
                case "application" -> result.setAppid("other-app");
                case "missing-order" -> result.setOutTradeNo(null);
                case "missing-merchant" -> result.setMchid(null);
                case "missing-application" -> result.setAppid(null);
                case "missing-state" -> result.setTradeState(null);
                case "empty" -> result = null;
                default -> throw new AssertionError(fault);
            }
            when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                    .thenReturn(result);

            assertThrows(IllegalStateException.class, () -> service.reconcile("XHPTEST01"), fault);
            assertThrows(IllegalStateException.class, () -> service.close("XHPTEST01"), fault);

            assertEquals("APPROVED", service.getForAccount("XHPTEST01", 7).status());
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        verify(nativePay, org.mockito.Mockito.never()).closeOrder(any(CloseOrderRequest.class));
    }

    @Test
    void matchingClosedQueryDoesNotRequireOptionalReceiptFields() {
        Transaction result = success(1234);
        result.setTradeState(Transaction.TradeStateEnum.CLOSED);
        result.setTransactionId(null);
        result.setAmount(null);
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(result);

        service.reconcile("XHPTEST01");

        assertEquals("CANCELLED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        verify(nativePay, org.mockito.Mockito.never()).closeOrder(any(CloseOrderRequest.class));
    }

    @Test
    void matchingUnpaidQueryDoesNotRequireOptionalReceiptFields() {
        Transaction result = success(1234);
        result.setTradeState(Transaction.TradeStateEnum.NOTPAY);
        result.setTransactionId(null);
        result.setAmount(null);
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenReturn(result);

        service.reconcile("XHPTEST01");

        assertEquals("APPROVED", service.getForAccount("XHPTEST01", 7).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
    }

    @Test
    void failedRecoveriesDoNotStarveLaterPendingPayments() {
        jdbc.update("UPDATE payment_order SET status='CANCELLED' WHERE payment_no='XHPTEST01'");
        java.time.LocalDateTime old = java.time.LocalDateTime.of(2020, 1, 1, 0, 0);
        for (int i = 1; i <= 31; i++) {
            String no = i == 31 ? "XHPLATE" : "XHPFAIL" + i;
            jdbc.update("""
                    INSERT INTO payment_order(payment_no,subject,amount,status,provider,updated_at)
                    VALUES (?,'测试服务',12.34,'APPROVED','WECHAT',?)
                    """, no, old.plusSeconds(i));
        }
        when(nativePay.queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class)))
                .thenAnswer(call -> {
                    QueryOrderByOutTradeNoRequest query = call.getArgument(0);
                    if (!"XHPLATE".equals(query.getOutTradeNo())) {
                        throw new IllegalStateException("gateway unavailable");
                    }
                    Transaction unpaid = success(1234);
                    unpaid.setOutTradeNo("XHPLATE");
                    unpaid.setTradeState(Transaction.TradeStateEnum.NOTPAY);
                    return unpaid;
                });

        service.recoverPending();
        verify(nativePay, org.mockito.Mockito.never()).queryOrderByOutTradeNo(
                org.mockito.ArgumentMatchers.argThat(q -> q != null && "XHPLATE".equals(q.getOutTradeNo())));

        service.recoverPending();

        verify(nativePay).queryOrderByOutTradeNo(
                org.mockito.ArgumentMatchers.argThat(q -> q != null && "XHPLATE".equals(q.getOutTradeNo())));
        verify(nativePay, times(60)).queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class));
        assertEquals(31, jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_order WHERE status='APPROVED'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment_transaction", Integer.class));
        verify(nativePay, org.mockito.Mockito.never()).closeOrder(any(CloseOrderRequest.class));
    }

    @Test
    void failedRecoveryDoesNotTouchConcurrentTerminalState() {
        java.time.LocalDateTime updated = java.time.LocalDateTime.of(2020, 1, 1, 0, 0);
        for (String terminal : new String[]{"CAPTURED", "REFUNDED", "CANCELLED"}) {
            jdbc.update("UPDATE payment_order SET status='APPROVED',updated_at=? WHERE payment_no='XHPTEST01'",
                    updated.minusSeconds(10));
            org.mockito.Mockito.doAnswer(call -> {
                // Simulate another transaction completing while the SDK request fails.
                jdbc.update("UPDATE payment_order SET status=?,updated_at=? WHERE payment_no='XHPTEST01'",
                        terminal, updated);
                throw new IllegalStateException("gateway unavailable");
            }).when(nativePay).queryOrderByOutTradeNo(any(QueryOrderByOutTradeNoRequest.class));

            service.recoverPending();

            assertEquals(terminal, service.getForAccount("XHPTEST01", 7).status());
            assertEquals(updated, jdbc.queryForObject(
                    "SELECT updated_at FROM payment_order WHERE payment_no='XHPTEST01'",
                    java.time.LocalDateTime.class));
        }
    }

    private void expirePending(String paymentNo) {
        jdbc.update("UPDATE payment_order SET native_expires_at=? WHERE payment_no=?",
                java.time.LocalDateTime.ofInstant(java.time.Instant.now().minusSeconds(60),
                        java.time.ZoneOffset.UTC), paymentNo);
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
