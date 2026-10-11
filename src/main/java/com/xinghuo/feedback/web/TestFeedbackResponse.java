package com.xinghuo.feedback.web;

import java.time.Instant;
import java.util.UUID;

public record TestFeedbackResponse(
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
