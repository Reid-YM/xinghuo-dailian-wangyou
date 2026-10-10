package com.xinghuo.quote.application;

import java.time.Instant;
import java.util.UUID;

/** Application-owned CRM projection of a quote lead. */
public record QuoteLeadView(
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
        Instant createdAt,
        Integer leadScore,
        String leadIntentLevel,
        String nextAction,
        String followUpNote,
        String aiSuggestion,
        Instant lastContactedAt,
        String firstTouchSource,
        String utmSource,
        String utmMedium,
        String utmCampaign,
        String referrer,
        Instant updatedAt,
        String customerSupplement) {
}
