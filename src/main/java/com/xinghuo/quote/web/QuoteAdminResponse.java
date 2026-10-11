package com.xinghuo.quote.web;

import java.time.Instant;
import java.util.UUID;

public record QuoteAdminResponse(
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
