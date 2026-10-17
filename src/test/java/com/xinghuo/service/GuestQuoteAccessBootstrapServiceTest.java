package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class GuestQuoteAccessBootstrapServiceTest {
    private JdbcTemplate jdbc;
    private GuestQuoteAccessBootstrapService service;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:guestbootstrap" + System.nanoTime()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
                CREATE TABLE quotes(
                  id VARCHAR(36) PRIMARY KEY,
                  business_no VARCHAR(40) UNIQUE,
                  status VARCHAR(24),
                  customer_account_id BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE guest_business_access(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  quote_id VARCHAR(36) UNIQUE,
                  business_no VARCHAR(40) UNIQUE,
                  status VARCHAR(16),
                  claimed_account_id BIGINT,
                  created_at TIMESTAMP,
                  updated_at TIMESTAMP)
                """);
        service = new GuestQuoteAccessBootstrapService(jdbc);
    }

    @Test
    void legacyAnonymousQuoteGetsStableGuestAccessIdempotently() {
        UUID quoteId = UUID.randomUUID();
        jdbc.update("INSERT INTO quotes(id,business_no,status,customer_account_id) VALUES(?,?,'NEW',NULL)",
                quoteId.toString(), "B-LEGACY");

        long first = service.ensureAnonymousGuestAccess("B-LEGACY");
        long second = service.ensureAnonymousGuestAccess(" B-LEGACY ");

        assertEquals(first, second);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM guest_business_access WHERE business_no='B-LEGACY'",
                Integer.class));
    }

    @Test
    void registeredQuoteNeverGetsGuestAccess() {
        UUID quoteId = UUID.randomUUID();
        jdbc.update("INSERT INTO quotes(id,business_no,status,customer_account_id) VALUES(?,?,'NEW',42)",
                quoteId.toString(), "B-ACCOUNT");

        assertThrows(BadRequestException.class,
                () -> service.ensureAnonymousGuestAccess("B-ACCOUNT"));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM guest_business_access WHERE business_no='B-ACCOUNT'",
                Integer.class));
    }

    @Test
    void missingQuoteIsNotBootstrapped() {
        assertThrows(NotFoundException.class,
                () -> service.ensureAnonymousGuestAccess("B-MISSING"));
    }
}
