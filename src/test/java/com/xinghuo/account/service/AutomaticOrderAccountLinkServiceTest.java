package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xinghuo.account.model.Account;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AutomaticOrderAccountLinkServiceTest {

    private JdbcTemplate jdbc;
    private AutomaticOrderAccountLinkService service;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:autoorderlink" + System.nanoTime()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
                CREATE TABLE customer_orders(
                    order_no VARCHAR(64) PRIMARY KEY,
                    business_no VARCHAR(64),
                    source_quote_id VARCHAR(36),
                    customer_account_id BIGINT,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE quotes(
                    id VARCHAR(36) PRIMARY KEY,
                    business_no VARCHAR(64) UNIQUE,
                    customer_account_id BIGINT,
                    customer_contact VARCHAR(120),
                    contact_method VARCHAR(40)
                )
                """);
        service = new AutomaticOrderAccountLinkService(jdbc);
    }

    @Test
    void adminReconcileUsesSourceQuoteEvenWhenBusinessNumbersDiffer() {
        insertQuote("Q-1", "B-QUOTE", 42L, "ignored", "手机");
        insertOrder("O-1", "B-ORDER", "Q-1", null);

        var result = service.reconcile("O-1");

        assertTrue(result.linked());
        assertEquals(42L, result.accountId());
        assertEquals("SOURCE_QUOTE_ACCOUNT", result.source());
        assertEquals(42L, owner("O-1"));
    }

    @Test
    void legacyOrderWithoutSourceQuoteMayFallBackToUnifiedBusinessNumber() {
        insertQuote("Q-2", "B-2", 88L, null, null);
        insertOrder("O-2", "B-2", null, null);

        var result = service.reconcile("B-2");

        assertTrue(result.linked());
        assertEquals(88L, result.accountId());
        assertEquals("LEGACY_BUSINESS_QUOTE_ACCOUNT", result.source());
        assertEquals(88L, owner("O-2"));
    }

    @Test
    void anonymousQuoteContactIsNeverUsedAsIdentityEvidence() {
        insertQuote("Q-3", "B-3", null, "13800138000", "手机");
        insertOrder("O-3", "B-3", "Q-3", null);

        var result = service.reconcile("O-3");

        assertFalse(result.linked());
        assertNull(result.accountId());
        assertNull(owner("O-3"));
    }

    @Test
    void orphanedSourceQuoteDoesNotFallBackByBusinessNumber() {
        insertQuote("Q-OTHER", "B-4", 42L, null, null);
        insertOrder("O-4", "B-4", "Q-MISSING", null);

        var result = service.reconcile("B-4");

        assertFalse(result.linked());
        assertNull(owner("O-4"));
    }

    @Test
    void existingOwnerIsNeverOverwritten() {
        insertQuote("Q-5", "B-5", 42L, null, null);
        insertOrder("O-5", "B-5", "Q-5", 7L);

        var result = service.reconcile("B-5");

        assertFalse(result.linked());
        assertEquals(7L, result.accountId());
        assertEquals("EXISTING", result.source());
        assertEquals(7L, owner("O-5"));
    }

    @Test
    void accountReconcileLinksOnlyQuoteLineageOwnedByThatAccount() {
        insertQuote("Q-6", "B-6", 55L, "other", "QQ");
        insertQuote("Q-7", "B-7", null, "13800138000", "手机");
        insertQuote("Q-8", "B-8", 66L, "13800138000", "手机");
        insertOrder("O-6", "B-6", "Q-6", null);
        insertOrder("O-7", "B-7", "Q-7", null);
        insertOrder("O-8", "B-8", "Q-8", null);

        Account account = Account.builder()
                .id(55L)
                .contactType(Account.ContactType.PHONE)
                .contactNorm("13800138000")
                .contactValue("13800138000")
                .contactVerified(true)
                .publicUserId("U123456789")
                .passwordHash("x")
                .displayName("tester")
                .role("BUYER")
                .levelCode("NEW")
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertEquals(1, service.reconcile(account));
        assertEquals(55L, owner("O-6"));
        assertNull(owner("O-7"));
        assertNull(owner("O-8"));
    }

    private void insertQuote(
            String id, String businessNo, Long ownerId, String contact, String method) {
        jdbc.update("""
                INSERT INTO quotes(id,business_no,customer_account_id,customer_contact,contact_method)
                VALUES(?,?,?,?,?)
                """, id, businessNo, ownerId, contact, method);
    }

    private void insertOrder(
            String orderNo, String businessNo, String sourceQuoteId, Long ownerId) {
        jdbc.update("""
                INSERT INTO customer_orders(
                  order_no,business_no,source_quote_id,customer_account_id,version,created_at,updated_at)
                VALUES(?,?,?,?,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, orderNo, businessNo, sourceQuoteId, ownerId);
    }

    private Long owner(String orderNo) {
        return jdbc.queryForObject(
                "SELECT customer_account_id FROM customer_orders WHERE order_no=?",
                Long.class,
                orderNo);
    }
}
