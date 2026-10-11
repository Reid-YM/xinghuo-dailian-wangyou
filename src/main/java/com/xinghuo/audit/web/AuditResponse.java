package com.xinghuo.audit.web;

import java.time.Instant;

public record AuditResponse(
        Long id,
        String action,
        String targetType,
        String targetId,
        String operatorName,
        String reason,
        String ipAddress,
        Instant createdAt) {
}
