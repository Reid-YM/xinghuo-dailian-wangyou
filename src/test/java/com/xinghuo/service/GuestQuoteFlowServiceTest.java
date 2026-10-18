package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.Quote;
import com.xinghuo.payment.GuestWeChatPaymentService;
import com.xinghuo.repository.QuoteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GuestQuoteFlowServiceTest {
    private JdbcTemplate jdbc;
    private QuoteRepository quotes;
    private OrderService orders;
    private AuditService audit;
    private ObjectProvider<GuestWeChatPaymentService> payments;
    private GuestQuoteFlowService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:guestflow" + System.nanoTime()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
                CREATE TABLE quotes(
                  id VARCHAR(36) PRIMARY KEY,business_no VARCHAR(40) UNIQUE,status VARCHAR(24),
                  customer_account_id BIGINT,game VARCHAR(80),demand VARCHAR(500),current_progress VARCHAR(240),
                  expected_time VARCHAR(80),updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE guest_session(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,token_hash VARCHAR(64) UNIQUE,status VARCHAR(16),
                  expires_at TIMESTAMP,last_seen_at TIMESTAMP,claimed_account_id BIGINT,
                  created_at TIMESTAMP,updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE guest_business_access(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,quote_id VARCHAR(36) UNIQUE,
                  business_no VARCHAR(40) UNIQUE,status VARCHAR(16),claimed_account_id BIGINT,
                  created_at TIMESTAMP,updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE guest_business_session_access(
                  guest_session_id BIGINT,guest_access_id BIGINT,created_at TIMESTAMP,
                  PRIMARY KEY(guest_session_id,guest_access_id))
                """);
        jdbc.execute("""
                CREATE TABLE guest_access_link(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,guest_access_id BIGINT,token_hash VARCHAR(64) UNIQUE,
                  expires_at TIMESTAMP,used_at TIMESTAMP,created_by VARCHAR(100),created_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE quote_offer(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,quote_id VARCHAR(36),business_no VARCHAR(40),version INT,
                  subject VARCHAR(127),amount DECIMAL(10,2),currency VARCHAR(8),estimated_duration VARCHAR(120),
                  customer_message VARCHAR(1000),status VARCHAR(16),quoted_by VARCHAR(100),quoted_at TIMESTAMP,
                  expires_at TIMESTAMP,accepted_at TIMESTAMP,cancelled_at TIMESTAMP,created_at TIMESTAMP,updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE customer_orders(
                  order_no VARCHAR(64) PRIMARY KEY,business_no VARCHAR(40),source_quote_id VARCHAR(36),
                  customer_account_id BIGINT,guest_access_id BIGINT,status VARCHAR(32),progress_percent INT,
                  progress_text VARCHAR(240),updated_at TIMESTAMP,finished_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE payment_order(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,payment_no VARCHAR(64),guest_access_id BIGINT,
                  business_no VARCHAR(40),subject VARCHAR(127),amount DECIMAL(10,2),currency VARCHAR(8),
                  status VARCHAR(24),provider VARCHAR(24),native_expires_at TIMESTAMP,paid_at TIMESTAMP,updated_at TIMESTAMP)
                """);

        quotes = mock(QuoteRepository.class);
        orders = mock(OrderService.class);
        audit = mock(AuditService.class);
        payments = mock(ObjectProvider.class);
        when(payments.getIfAvailable()).thenReturn(null);
        service = new GuestQuoteFlowService(jdbc, quotes, orders, audit, payments);
    }

    @Test
    void oneBrowserSessionCanOwnMultipleAnonymousQuotes() {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        insertQuote(q1, "B-1");
        insertQuote(q2, "B-2");

        GuestQuoteFlowService.SessionHandle session = service.ensureSession(null);
        service.attachQuote(session.id(), q1, "B-1");
        service.attachQuote(session.id(), q2, "B-2");

        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM guest_business_session_access
                 WHERE guest_session_id=?
                """, Integer.class, session.id()));
        assertEquals("B-1", service.snapshot(session.token(), "B-1").get("businessNo"));
        assertEquals("B-2", service.snapshot(session.token(), "B-2").get("businessNo"));
        assertThrows(NotFoundException.class,
                () -> service.snapshot("x".repeat(43), "B-1"));
    }

    @Test
    void existingQuoteCannotBeStolenByASecondSessionWithoutOneTimeLink() {
        UUID quoteId = UUID.randomUUID();
        insertQuote(quoteId, "B-LOCK");
        GuestQuoteFlowService.SessionHandle first = service.ensureSession(null);
        GuestQuoteFlowService.SessionHandle second = service.ensureSession(null);
        service.attachQuote(first.id(), quoteId, "B-LOCK");

        assertThrows(ConflictException.class,
                () -> service.attachQuote(second.id(), quoteId, "B-LOCK"));
        assertEquals("B-LOCK", service.snapshot(first.token(), "B-LOCK").get("businessNo"));
        assertThrows(NotFoundException.class,
                () -> service.snapshot(second.token(), "B-LOCK"));
    }

    @Test
    void oneTimeLinkAuthorizesSecondBrowserWithoutChangingStableGuestAccess() {
        UUID quoteId = UUID.randomUUID();
        insertQuote(quoteId, "B-SHARE");
        GuestQuoteFlowService.SessionHandle first = service.ensureSession(null);
        service.attachQuote(first.id(), quoteId, "B-SHARE");
        Long accessBefore = jdbc.queryForObject(
                "SELECT id FROM guest_business_access WHERE business_no='B-SHARE'", Long.class);

        GuestQuoteFlowService.AccessLink link = service.issueAccessLink("B-SHARE", "admin");
        String rawToken = link.path().substring(link.path().lastIndexOf('/') + 1);
        GuestQuoteFlowService.ExchangeResult exchanged = service.exchangeAccessLink(rawToken, null);

        assertNotEquals(first.id(), exchanged.session().id());
        assertEquals("B-SHARE", exchanged.businessNo());
        assertEquals("B-SHARE", service.snapshot(exchanged.session().token(), "B-SHARE").get("businessNo"));
        assertEquals(accessBefore, jdbc.queryForObject(
                "SELECT id FROM guest_business_access WHERE business_no='B-SHARE'", Long.class));
        assertEquals(2, jdbc.queryForObject("""
                SELECT COUNT(*) FROM guest_business_session_access WHERE guest_access_id=?
                """, Integer.class, accessBefore));
        assertThrows(NotFoundException.class,
                () -> service.exchangeAccessLink(rawToken, null));
    }

    @Test
    void republishingOfferCancelsPreviousVersionButKeepsHistory() {
        UUID quoteId = UUID.randomUUID();
        insertQuote(quoteId, "B-3");
        GuestQuoteFlowService.SessionHandle session = service.ensureSession(null);
        service.attachQuote(session.id(), quoteId, "B-3");

        service.publishOffer("B-3", offer("100.00", "第一版"), "admin", null);
        service.publishOffer("B-3", offer("120.00", "第二版"), "admin", null);

        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM quote_offer WHERE business_no='B-3'", Integer.class));
        assertEquals("CANCELLED", jdbc.queryForObject(
                "SELECT status FROM quote_offer WHERE business_no='B-3' AND version=1", String.class));
        assertEquals("QUOTED", jdbc.queryForObject(
                "SELECT status FROM quote_offer WHERE business_no='B-3' AND version=2", String.class));
        assertEquals("QUOTED", jdbc.queryForObject(
                "SELECT status FROM quotes WHERE business_no='B-3'", String.class));
    }

    @Test
    void acceptingOfferCreatesGuestOwnedOrderAndConvertsQuoteOnce() {
        UUID quoteId = UUID.randomUUID();
        insertQuote(quoteId, "B-4");
        GuestQuoteFlowService.SessionHandle session = service.ensureSession(null);
        service.attachQuote(session.id(), quoteId, "B-4");
        service.publishOffer("B-4", offer("88.00", "星火服务"), "admin", null);

        Quote quote = Quote.builder()
                .id(quoteId)
                .businessNo("B-4")
                .game("MW4")
                .demand("测试需求")
                .status(Quote.Status.QUOTED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(quotes.findById(quoteId)).thenReturn(java.util.Optional.of(quote));
        when(quotes.save(any(Quote.class))).thenAnswer(invocation -> {
            Quote saved = invocation.getArgument(0);
            jdbc.update("UPDATE quotes SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                    saved.getStatus().name(), saved.getId().toString());
            return saved;
        });
        doAnswer(invocation -> {
            jdbc.update("""
                    INSERT INTO customer_orders(
                      order_no,business_no,source_quote_id,customer_account_id,guest_access_id,status,
                      progress_percent,progress_text,updated_at)
                    VALUES('O-4','B-4',?,NULL,NULL,'WAITING_CONFIRMATION',NULL,'等待确认',CURRENT_TIMESTAMP)
                    """, quoteId.toString());
            return new ApiDtos.AdminOrderResponse(
                    UUID.randomUUID(), "O-4", "B-4", quoteId, "WAITING_CONFIRMATION",
                    null, "等待确认", false, null, null, Instant.now(), 0L);
        }).when(orders).createFromQuote(any(Quote.class), anyString(), any(), anyString());

        var result = service.accept(session.token(), "B-4", null);

        Long guestAccessId = jdbc.queryForObject(
                "SELECT guest_access_id FROM customer_orders WHERE order_no='O-4'", Long.class);
        assertNotNull(guestAccessId);
        assertEquals("ACCEPTED", jdbc.queryForObject(
                "SELECT status FROM quote_offer WHERE business_no='B-4'", String.class));
        assertEquals("CONVERTED", jdbc.queryForObject(
                "SELECT status FROM quotes WHERE business_no='B-4'", String.class));
        assertNotNull(result.get("order"));
        verify(quotes).save(quote);

        service.accept(session.token(), "B-4", null);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_orders WHERE business_no='B-4'", Integer.class));
    }

    @Test
    void staffConvertedUnpaidGuestOrderCanBeQuotedAcceptedAndPaidWithoutDuplicateOrder() {
        UUID quoteId = UUID.randomUUID();
        insertQuote(quoteId, "B-ORDERFIRST");
        GuestQuoteFlowService.SessionHandle session = service.ensureSession(null);
        service.attachQuote(session.id(), quoteId, "B-ORDERFIRST");
        jdbc.update("UPDATE quotes SET status='CONVERTED' WHERE id=?", quoteId.toString());
        jdbc.update("""
                INSERT INTO customer_orders(order_no,business_no,source_quote_id,
                  customer_account_id,guest_access_id,status,progress_percent,progress_text,updated_at)
                VALUES('O-ORDERFIRST','B-ORDERFIRST',?,NULL,NULL,'WAITING_CONFIRMATION',
                       NULL,'等待客服报价',CURRENT_TIMESTAMP)
                """, quoteId.toString());

        service.publishOffer("B-ORDERFIRST", offer("168.00", "已转单补价"), "admin", null);
        assertEquals("CONVERTED", jdbc.queryForObject(
                "SELECT status FROM quotes WHERE id=?", String.class, quoteId.toString()));
        assertEquals(true, service.snapshot(session.token(), "B-ORDERFIRST").get("canAccept"));

        Quote quote = Quote.builder().id(quoteId).businessNo("B-ORDERFIRST")
                .game("MW4").demand("测试需求").status(Quote.Status.CONVERTED)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(quotes.findById(quoteId)).thenReturn(java.util.Optional.of(quote));
        ApiDtos.AdminOrderResponse original = new ApiDtos.AdminOrderResponse(
                UUID.randomUUID(), "O-ORDERFIRST", "B-ORDERFIRST", quoteId,
                "WAITING_CONFIRMATION", null, "等待客服报价", false,
                null, null, Instant.now(), 0L);
        when(orders.createFromQuote(any(Quote.class), anyString(), any(), anyString()))
                .thenReturn(original);

        var accepted = service.accept(session.token(), "B-ORDERFIRST", null);
        Long accessId = jdbc.queryForObject(
                "SELECT guest_access_id FROM customer_orders WHERE business_no='B-ORDERFIRST'",
                Long.class);
        assertNotNull(accessId);
        assertEquals("ACCEPTED", jdbc.queryForObject(
                "SELECT status FROM quote_offer WHERE business_no='B-ORDERFIRST'",
                String.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_orders WHERE business_no='B-ORDERFIRST'",
                Integer.class));
        assertNotNull(accepted.get("order"));
        service.accept(session.token(), "B-ORDERFIRST", null);
        verify(orders).createFromQuote(any(Quote.class), anyString(), any(), anyString());

        // If WeChat pay was disabled at the original acceptance, a later enabled gateway
        // can safely prepare the same accepted amount for the already-linked guest order.
        GuestWeChatPaymentService gateway = mock(GuestWeChatPaymentService.class);
        when(payments.getIfAvailable()).thenReturn(gateway);
        var intent = new com.xinghuo.payment.WeChatNativePaymentService.Payment(
                "P-ORDERFIRST", "B-ORDERFIRST", "已转单补价", new BigDecimal("168.00"),
                "CREATED", null, null);
        when(gateway.latestForGuest("B-ORDERFIRST", accessId)).thenReturn(null);
        when(gateway.prepare("O-ORDERFIRST", accessId, new BigDecimal("168.00"), "已转单补价"))
                .thenReturn(intent);
        when(gateway.nativeQr("P-ORDERFIRST", accessId)).thenReturn(intent);
        assertEquals("P-ORDERFIRST",
                service.nativePayment(session.token(), "B-ORDERFIRST").paymentNo());
        verify(gateway).prepare("O-ORDERFIRST", accessId, new BigDecimal("168.00"), "已转单补价");
    }

    @Test
    void paidOrPendingGuestPaymentPreventsPostConversionRepricing() {
        UUID quoteId = UUID.randomUUID();
        insertQuote(quoteId, "B-PAID");
        GuestQuoteFlowService.SessionHandle session = service.ensureSession(null);
        service.attachQuote(session.id(), quoteId, "B-PAID");
        jdbc.update("UPDATE quotes SET status='CONVERTED' WHERE id=?", quoteId.toString());
        jdbc.update("""
                INSERT INTO customer_orders(order_no,business_no,source_quote_id,
                  customer_account_id,guest_access_id,status,progress_percent,progress_text,updated_at)
                VALUES('O-PAID','B-PAID',?,NULL,NULL,'WAITING_CONFIRMATION',
                       NULL,'等待客服报价',CURRENT_TIMESTAMP)
                """, quoteId.toString());
        jdbc.update("""
                INSERT INTO payment_order(payment_no,guest_access_id,business_no,subject,
                  amount,status,provider,updated_at)
                VALUES('P-PAID',NULL,'B-PAID','旧支付单',100.00,'APPROVED','WECHAT',CURRENT_TIMESTAMP)
                """);
        assertThrows(ConflictException.class,
                () -> service.publishOffer("B-PAID", offer("120.00", "新价"), "admin", null));
        jdbc.update("UPDATE payment_order SET status='CAPTURED' WHERE business_no='B-PAID'");
        assertThrows(ConflictException.class,
                () -> service.publishOffer("B-PAID", offer("120.00", "新价"), "admin", null));
    }

    @Test
    void noPaymentIntentCanBeMadeBeforeGuestAcceptsAnOffer() {
        UUID quoteId = UUID.randomUUID();
        insertQuote(quoteId, "B-CONSENT");
        GuestQuoteFlowService.SessionHandle session = service.ensureSession(null);
        service.attachQuote(session.id(), quoteId, "B-CONSENT");
        GuestWeChatPaymentService gateway = mock(GuestWeChatPaymentService.class);
        when(payments.getIfAvailable()).thenReturn(gateway);
        assertThrows(ConflictException.class,
                () -> service.nativePayment(session.token(), "B-CONSENT"));
        verify(gateway, org.mockito.Mockito.never()).prepare(anyString(),
                org.mockito.ArgumentMatchers.anyLong(), any(), anyString());
    }

    private void insertQuote(UUID id, String businessNo) {
        jdbc.update("""
                INSERT INTO quotes(
                  id,business_no,status,customer_account_id,game,demand,current_progress,expected_time,updated_at)
                VALUES(?,?,'NEW',NULL,'MW4','测试需求',NULL,NULL,CURRENT_TIMESTAMP)
                """, id.toString(), businessNo);
    }

    private GuestQuoteFlowService.OfferRequest offer(String amount, String subject) {
        return new GuestQuoteFlowService.OfferRequest(
                new BigDecimal(amount), subject, "1-2 天", "客服说明",
                Instant.now().plusSeconds(3600), "测试报价");
    }
}
