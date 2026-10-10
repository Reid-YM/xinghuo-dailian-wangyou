package com.xinghuo.quote.infrastructure;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.quote.application.QuoteLegacyPort;
import com.xinghuo.quote.application.QuoteSubmissionCommand;
import com.xinghuo.quote.application.QuoteSubmissionResult;
import com.xinghuo.service.QuoteService;
import org.springframework.stereotype.Component;

/** Bridges the application-owned quote contract to the existing implementation. */
@Component
public class LegacyQuoteAdapter implements QuoteLegacyPort {
    private final QuoteService legacy;

    public LegacyQuoteAdapter(QuoteService legacy) {
        this.legacy = legacy;
    }

    @Override
    public QuoteSubmissionResult create(QuoteSubmissionCommand command) {
        PublicQuoteRequest request = new PublicQuoteRequest(
                command.game(),
                command.demand(),
                command.currentProgress(),
                command.screenshotAssetId(),
                command.platform(),
                command.expectedTime(),
                command.contactMethod(),
                command.customerContact(),
                command.privacyConsent(),
                command.firstTouchSource(),
                command.utmSource(),
                command.utmMedium(),
                command.utmCampaign(),
                command.referrer());
        ApiDtos.QuoteResponse response = legacy.create(
                request,
                command.idempotencyKey(),
                command.customerIpHash(),
                command.customerAccountId());
        return new QuoteSubmissionResult(
                response.id(), response.businessNo(), response.status(), response.createdAt());
    }
}
