package com.xinghuo.record.application;

import java.time.Instant;

/** Application-owned unified public record projection. */
public record RecordPublicView(
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
