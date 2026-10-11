package com.xinghuo.order.web;

import java.time.Instant;

public record OrderPublicResponse(
        String businessNo,
        String status,
        Integer progressPercent,
        String progressText,
        Instant updatedAt,
        boolean hasProgressImage,
        String customerNote) {
}
