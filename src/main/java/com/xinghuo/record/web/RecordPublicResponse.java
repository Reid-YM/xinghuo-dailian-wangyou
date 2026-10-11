package com.xinghuo.record.web;

import java.time.Instant;

public record RecordPublicResponse(
        String businessNo,
        String stage,
        String status,
        String title,
        String expectedTime,
        Integer progressPercent,
        String progressText,
        Instant createdAt,
        Instant updatedAt,
        boolean hasProgressImage,
        String customerNote) {
}
