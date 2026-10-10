package com.xinghuo.audit.application;

import java.time.Instant;

public record AuditEntryView(
        Long id,
        String action,
        String targetType,
        String targetId,
        String operatorName,
        String reason,
        String ipAddress,
        Instant createdAt) {
}
