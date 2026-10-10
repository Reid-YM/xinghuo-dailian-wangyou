package com.xinghuo.quote.domain;

/**
 * Request-independent metadata required to submit a quote.
 *
 * <p>The HTTP layer is responsible for deriving these values. Keeping them in
 * the quote domain boundary prevents application code from depending on the
 * servlet request itself while the legacy quote implementation is migrated.</p>
 */
public record QuoteSubmissionMetadata(
        String idempotencyKey,
        String customerIpHash,
        Long customerAccountId) {
}
