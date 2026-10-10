package com.xinghuo.quote.infrastructure;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.quote.application.QuoteLegacyPort;
import com.xinghuo.quote.domain.QuoteSubmissionMetadata;
import com.xinghuo.service.QuoteService;
import org.springframework.stereotype.Component;

/** Bridges the new quote boundary to the existing implementation during migration. */
@Component
public class LegacyQuoteAdapter implements QuoteLegacyPort {
    private final QuoteService legacy;

    public LegacyQuoteAdapter(QuoteService legacy) {
        this.legacy = legacy;
    }

    @Override
    public ApiDtos.QuoteResponse create(PublicQuoteRequest request, QuoteSubmissionMetadata metadata) {
        return legacy.create(
                request,
                metadata.idempotencyKey(),
                metadata.customerIpHash(),
                metadata.customerAccountId());
    }
}
