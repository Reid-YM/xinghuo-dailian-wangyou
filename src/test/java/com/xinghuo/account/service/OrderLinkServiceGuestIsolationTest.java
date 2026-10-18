package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.*;

import com.xinghuo.account.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class OrderLinkServiceGuestIsolationTest {
    JdbcTemplate jdbc;
    OrderLinkService service;
    Account account;

    @BeforeEach
    void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:guestclaim" + System.nanoTime()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE quotes(
                    id VARCHAR(36) PRIMARY KEY, business_no VARCHAR(40),
                    customer_account_id BIGINT, game VARCHAR(30), demand VARCHAR(60),
                    customer_contact VARCHAR(120), contact_method VARCHAR(40))
                """);
        jdbc.execute("""
                CREATE TABLE customer_orders(
                    order_no VARCHAR(40) PRIMARY KEY, business_no VARCHAR(40),
                    source_quote_id VARCHAR(36), customer_account_id BIGINT,
                    guest_access_id BIGINT, version BIGINT NOT NULL DEFAULT 0,
                    status VARCHAR(24), progress_percent INT, progress_text VARCHAR(240),
                    created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO quotes(id,business_no,game,demand,customer_contact,contact_method)
                VALUES('Q-GUEST','B-GUEST','MW4','游戏服务','13800138000','手机')
                """);
        jdbc.update("""
                INSERT INTO customer_orders(order_no,business_no,source_quote_id,guest_access_id,
                                           status,created_at,updated_at)
                VALUES('ORD-GUEST','B-GUEST','Q-GUEST',123,'IN_PROGRESS',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
        account = Account.builder()
                .id(1L)
                .contactType(Account.ContactType.PHONE)
                .contactValue("13800138000")
                .contactNorm("13800138000")
                .contactVerified(true)
                .build();
        service = new OrderLinkService(jdbc);
    }

    @Test
    void contactMatchingCannotListOrClaimAlreadyOwnedGuestOrder() {
        assertTrue(service.listCandidates(account).isEmpty());
        AccountException failure = assertThrows(AccountException.class,
                () -> service.claim(account, "ORD-GUEST"));
        assertEquals("ORDER_ALREADY_LINKED", failure.getCode());
        assertNull(jdbc.queryForObject(
                "SELECT customer_account_id FROM customer_orders WHERE order_no='ORD-GUEST'",
                Long.class));
        assertEquals(123L, jdbc.queryForObject(
                "SELECT guest_access_id FROM customer_orders WHERE order_no='ORD-GUEST'",
                Long.class));
    }

    @Test
    void contactMatchingStillSupportsLegacyUnownedNonGuestOrder() {
        jdbc.update("UPDATE customer_orders SET guest_access_id=NULL WHERE order_no='ORD-GUEST'");
        assertEquals(1, service.listCandidates(account).size());
        assertEquals("ORD-GUEST", service.claim(account, "ORD-GUEST").orderNo());
        assertEquals(1L, jdbc.queryForObject(
                "SELECT customer_account_id FROM customer_orders WHERE order_no='ORD-GUEST'",
                Long.class));
    }
}
