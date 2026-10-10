package com.xinghuo.feedback.application;

import java.time.Instant;
import java.util.UUID;

/** Application-owned administrative test-feedback projection. */
public record TestFeedbackView(
        Long id,
        String title,
        String pagePath,
        String content,
        String priority,
        String status,
        String operatorNote,
        UUID attachmentAssetId,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
