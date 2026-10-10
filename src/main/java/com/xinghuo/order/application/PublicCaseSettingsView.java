package com.xinghuo.order.application;

/** Application-owned result for public-case administration. */
public record PublicCaseSettingsView(
        String orderNo,
        boolean allowPublicCase,
        String publicSummary) {
}
