package com.xinghuo.quote.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class QuoteLeadDtos {
    private QuoteLeadDtos() {
    }

    public record QuoteAiSuggestionResponse(Integer leadScore, String leadIntentLevel, String nextAction, String reply, String reason) {
    }

    public record QuoteCrmUpdateRequest(@Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer leadScore, @Pattern(regexp="LOW|MEDIUM|HIGH|UNKNOWN") @Pattern(regexp="LOW|MEDIUM|HIGH|UNKNOWN") String leadIntentLevel, @Size(max=240) @Size(max=240) String nextAction, @Size(max=5000) @Size(max=5000) String followUpNote, @Size(max=500) @Size(max=500) String reason) {
    }

    public record QuoteLeadPageResponse(List<QuoteLeadResponse> items, int page, int size, long total, int totalPages, List<String> sources) {
    }

    public record QuoteLeadResponse(UUID id, String businessNo, String game, String demand, String currentProgress, UUID screenshotAssetId, String platform, String expectedTime, String contactMethod, String customerContact, String customerIpHash, String status, Instant createdAt, Integer leadScore, String leadIntentLevel, String nextAction, String followUpNote, String aiSuggestion, Instant lastContactedAt, String firstTouchSource, String utmSource, String utmMedium, String utmCampaign, String referrer, Instant updatedAt, String customerSupplement) {
    }
}
