package com.xinghuo.quote.application;

import java.time.Instant;
import java.util.UUID;

/** Application-owned projection returned after converting a quote into an order. */
public record ConvertedOrderView(
        UUID id,
        String orderNo,
        String businessNo,
        UUID sourceQuoteId,
        String status,
        Integer progressPercent,
        String progressText,
        boolean hasProgressImage,
        String customerNote,
        String internalNote,
        Instant updatedAt,
        long version) {
}
