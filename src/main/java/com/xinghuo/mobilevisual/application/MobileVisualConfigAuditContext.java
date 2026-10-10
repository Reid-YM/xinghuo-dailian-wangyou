package com.xinghuo.mobilevisual.application;

public record MobileVisualConfigAuditContext(
        String operator,
        String reason,
        String ipAddress,
        String requestId) {
}
