package com.xinghuo.quote.application;

import java.util.UUID;

/** Application-level input for the public quote use case. */
public record QuoteSubmissionCommand(
        String game,
        String demand,
        String currentProgress,
        UUID screenshotAssetId,
        String platform,
        String expectedTime,
        String contactMethod,
        String customerContact,
        Boolean privacyConsent,
        String firstTouchSource,
        String utmSource,
        String utmMedium,
        String utmCampaign,
        String referrer,
        String idempotencyKey,
        String customerIpHash,
        Long customerAccountId) {
}
