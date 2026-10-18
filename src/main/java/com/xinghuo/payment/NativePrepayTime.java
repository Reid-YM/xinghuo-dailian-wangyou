package com.xinghuo.payment;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * WeChat Native /v3/pay/transactions/native time_expire contract.
 *
 * <p>Wechat's example uses yyyy-MM-dd'T'HH:mm:ss+08:00. Formatting a raw
 * Instant.now() using ISO_OFFSET_DATE_TIME preserves fractional seconds;
 * such fractional digits may be rejected as PARAM_ERROR by the gateway.
 * Keep the absolute expiry and its serialized value at the same precision.
 */
final class NativePrepayTime {
    private static final ZoneOffset WECHAT_OFFSET = ZoneOffset.ofHours(8);

    private NativePrepayTime() {}

    static Instant afterMinutes(Instant now, long minutes) {
        return now.plusSeconds(Math.multiplyExact(minutes, 60L))
                .truncatedTo(ChronoUnit.SECONDS);
    }

    static String timeExpire(Instant expiry) {
        return expiry.truncatedTo(ChronoUnit.SECONDS)
                .atOffset(WECHAT_OFFSET)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
