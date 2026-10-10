package com.xinghuo.frontendcontrol.application;

public record FrontendControlAuditContext(
        String operator,
        String reason,
        String ipAddress,
        String requestId) {
}
