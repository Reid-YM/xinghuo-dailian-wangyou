package com.xinghuo.quote.application;

import java.time.Instant;
import java.util.UUID;

/** Application-level result for a submitted quote. */
public record QuoteSubmissionResult(
        UUID id,
        String businessNo,
        String status,
        Instant createdAt) {
}
