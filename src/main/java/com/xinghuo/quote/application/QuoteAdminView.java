package com.xinghuo.quote.application;

import java.time.Instant;
import java.util.UUID;

/** Application-owned administrative projection of a quote. */
public record QuoteAdminView(
        UUID id,
        String businessNo,
        String game,
        String demand,
        String currentProgress,
        UUID screenshotAssetId,
        String platform,
        String expectedTime,
        String contactMethod,
        String customerContact,
        String customerIpHash,
        String status,
        Instant createdAt) {
}
