package com.xinghuo.quote.application;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.PublicQuoteRequest;
import com.xinghuo.quote.domain.QuoteSubmissionMetadata;

/**
 * Temporary migration port between the quote application layer and the legacy
 * implementation. #128 can replace the adapter without changing web callers.
 */
public interface QuoteLegacyPort {
    ApiDtos.QuoteResponse create(PublicQuoteRequest request, QuoteSubmissionMetadata metadata);
}
