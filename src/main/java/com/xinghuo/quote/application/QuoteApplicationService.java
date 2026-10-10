package com.xinghuo.quote.application;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.quote.domain.QuoteSubmissionMetadata;
import org.springframework.stereotype.Service;

/**
 * Application entry point for public quote submission.
 *
 * <p>During #127 it delegates through a migration port so existing quote
 * behavior stays stable. Later refactors can move orchestration and domain
 * rules behind this boundary without changing controllers.</p>
 */
@Service
public class QuoteApplicationService {
    private final QuoteLegacyPort legacy;

    public QuoteApplicationService(QuoteLegacyPort legacy) {
        this.legacy = legacy;
    }

    public ApiDtos.QuoteResponse create(
            PublicQuoteRequest request,
            String idempotencyKey,
            String customerIpHash,
            Long customerAccountId) {
        return legacy.create(
                request,
                new QuoteSubmissionMetadata(idempotencyKey, customerIpHash, customerAccountId));
    }
}
