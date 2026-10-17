package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.*;

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
                    customer_account_id BIGINT,
                    version BIGINT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE quotes(
                    business_no VARCHAR(64) PRIMARY KEY,
                    customer_account_id BIGINT,
                    customer_contact VARCHAR(120),
                    contact_method VARCHAR(40)
                )
                """);
        jdbc.execute("""
                CREATE TABLE customer_accounts(
                    id BIGINT PRIMARY KEY,
                    contact_type VARCHAR(16) NOT NULL,
                    contact_norm VARCHAR(120) NOT NULL,
                    contact_verified BOOLEAN NOT NULL,
                    status VARCHAR(16) NOT NULL,
                    deleted_at TIMESTAMP NULL
                )
                """);
        service = new AutomaticOrderAccountLinkService(jdbc);
    }

    @Test
    void adminReconcileTrustsExistingQuoteAccountLineage() {
        insertOrder("O-1", "B-1", null);
        jdbc.update("INSERT INTO quotes VALUES('B-1',42,'ignored','手机')");

        var result = service.reconcile("B-1");

        assertTrue(result.linked());
        assertEquals(42L, result.accountId());
        assertEquals("QUOTE_ACCOUNT", result.source());
        assertEquals(42L, owner("O-1"));
    }

    @Test
    void adminReconcileFindsSingleVerifiedPrimaryContact() {
        insertOrder("O-2", "B-2", null);
        jdbc.update("INSERT INTO quotes VALUES('B-2',NULL,'+86 138-0013-8000','手机')");
        jdbc.update("INSERT INTO customer_accounts VALUES(88,'PHONE','13800138000',TRUE,'ACTIVE',NULL)");

        var result = service.reconcile("O-2");

        assertTrue(result.linked());
        assertEquals(88L, result.accountId());
        assertEquals("VERIFIED_PRIMARY_CONTACT", result.source());
        assertEquals(88L, owner("O-2"));
        assertEquals(88L, jdbc.queryForObject(
                "SELECT customer_account_id FROM quotes WHERE business_no='B-2'", Long.class));
    }

    @Test
    void adminReconcileDoesNotTrustUnverifiedOrInactiveContact() {
        insertOrder("O-3", "B-3", null);
        jdbc.update("INSERT INTO quotes VALUES('B-3',NULL,'13800138000','手机')");
        jdbc.update("INSERT INTO customer_accounts VALUES(99,'PHONE','13800138000',FALSE,'ACTIVE',NULL)");

        var result = service.reconcile("B-3");

        assertFalse(result.linked());
        assertNull(owner("O-3"));
    }

    @Test
    void existingOwnerIsNeverOverwritten() {
        insertOrder("O-4", "B-4", 7L);
        jdbc.update("INSERT INTO quotes VALUES('B-4',42,'13800138000','手机')");
        jdbc.update("INSERT INTO customer_accounts VALUES(42,'PHONE','13800138000',TRUE,'ACTIVE',NULL)");

        var result = service.reconcile("B-4");

        assertFalse(result.linked());
        assertEquals(7L, result.accountId());
        assertEquals(7L, owner("O-4"));
    }

    @Test
    void customerOrderListReconcileUsesLineageAndVerifiedContactAutomatically() {
        insertOrder("O-5", "B-5", null);
        insertOrder("O-6", "B-6", null);
        insertOrder("O-7", "B-7", null);
        jdbc.update("INSERT INTO quotes VALUES('B-5',55,'other','QQ')");
        jdbc.update("INSERT INTO quotes VALUES('B-6',NULL,'13800138000','手机')");
        jdbc.update("INSERT INTO quotes VALUES('B-7',NULL,'13900139000','手机')");

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

        assertEquals(2, service.reconcile(account));
        assertEquals(55L, owner("O-5"));
        assertEquals(55L, owner("O-6"));
        assertNull(owner("O-7"));
    }

    private void insertOrder(String orderNo, String businessNo, Long ownerId) {
        jdbc.update(
                "INSERT INTO customer_orders(order_no,business_no,customer_account_id,version,created_at,updated_at) VALUES(?,?,?,0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                orderNo, businessNo, ownerId);
    }

    private Long owner(String orderNo) {
        return jdbc.queryForObject(
                "SELECT customer_account_id FROM customer_orders WHERE order_no=?",
                Long.class,
                orderNo);
    }
}
