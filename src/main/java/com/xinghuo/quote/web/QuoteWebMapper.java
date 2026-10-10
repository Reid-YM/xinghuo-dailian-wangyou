package com.xinghuo.quote.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.quote.application.QuoteSubmissionCommand;
import com.xinghuo.quote.application.QuoteSubmissionResult;

/** Maps HTTP-facing quote DTOs to and from application-owned contracts. */
public final class QuoteWebMapper {
    private QuoteWebMapper() {
    }

    public static QuoteSubmissionCommand toCommand(
            PublicQuoteRequest request,
            String idempotencyKey,
            String customerIpHash,
            Long customerAccountId) {
        return new QuoteSubmissionCommand(
                request.game(),
                request.demand(),
                request.currentProgress(),
                request.screenshotAssetId(),
                request.platform(),
                request.expectedTime(),
                request.contactMethod(),
                request.customerContact(),
                request.privacyConsent(),
                request.firstTouchSource(),
                request.utmSource(),
                request.utmMedium(),
                request.utmCampaign(),
                request.referrer(),
                idempotencyKey,
                customerIpHash,
                customerAccountId);
    }

    public static ApiDtos.QuoteResponse toResponse(QuoteSubmissionResult result) {
        return new ApiDtos.QuoteResponse(
                result.id(), result.businessNo(), result.status(), result.createdAt());
    }
}
