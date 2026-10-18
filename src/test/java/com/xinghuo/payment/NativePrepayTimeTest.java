package com.xinghuo.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Regression: the WeChat Native time_expire field must not contain nanoseconds. */
class NativePrepayTimeTest {
    @Test
    void dateTimeUsesTheSecondPrecisionAndBeijingOffsetRequiredByWeChatNative() {
        Instant expiry = Instant.parse("2026-10-18T01:31:04.123456789Z");
        String formatted = NativePrepayTime.timeExpire(expiry);
        assertEquals("2026-10-18T09:31:04+08:00", formatted);
        assertFalse(formatted.contains("."));
        assertTrue(formatted.matches(
                "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+08:00"));
    }

    @Test
    void merchantAndDatabaseUseTheSameAbsoluteExpiryInstant() {
        Instant now = Instant.parse("2026-10-18T01:01:04.987654321Z");
        Instant expiry = NativePrepayTime.afterMinutes(now, 30);
        assertEquals(Instant.parse("2026-10-18T01:31:04Z"), expiry);
        assertEquals(expiry, OffsetDateTime.parse(NativePrepayTime.timeExpire(expiry)).toInstant());
        assertEquals(0, expiry.getNano());
    }
}
