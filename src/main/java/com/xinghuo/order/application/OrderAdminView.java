package com.xinghuo.order.application;

import java.time.Instant;
import java.util.UUID;

/** Framework-neutral order view for administration use cases. */
public record OrderAdminView(
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
