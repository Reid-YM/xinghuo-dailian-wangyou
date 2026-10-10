package com.xinghuo.sitesetting.application;

public record GlobalUiSettingsAuditContext(
        String operator,
        String reason,
        String ipAddress,
        String requestId) {
}
