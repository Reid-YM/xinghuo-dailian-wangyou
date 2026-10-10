package com.xinghuo.order.application;

import java.time.Instant;
import java.util.UUID;

/** Application-owned administrative order projection. */
public record AdminOrderView(
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
