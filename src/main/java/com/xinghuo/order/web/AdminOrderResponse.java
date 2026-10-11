package com.xinghuo.order.web;

import java.time.Instant;
import java.util.UUID;

public record AdminOrderResponse(
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
