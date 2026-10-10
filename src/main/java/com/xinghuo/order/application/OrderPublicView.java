package com.xinghuo.order.application;

import java.time.Instant;

/** Application-owned projection for public order progress. */
public record OrderPublicView(
        String businessNo,
        String status,
        Integer progressPercent,
        String progressText,
        Instant updatedAt,
        boolean hasProgressImage,
        String customerNote) {
}
