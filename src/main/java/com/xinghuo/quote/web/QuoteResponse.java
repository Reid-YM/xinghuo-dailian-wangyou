package com.xinghuo.quote.web;

import java.time.Instant;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        String businessNo,
        String status,
        Instant createdAt) {
}
