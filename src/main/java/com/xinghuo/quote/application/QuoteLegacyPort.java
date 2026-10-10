package com.xinghuo.quote.application;

/**
 * Temporary migration port between the quote application layer and legacy
 * persistence/orchestration. It exposes only application-owned contracts.
 */
public interface QuoteLegacyPort {
    QuoteSubmissionResult create(QuoteSubmissionCommand command);
}
