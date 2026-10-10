package com.xinghuo.order.application;

/** Framework-neutral result for toggling an order's public-case visibility. */
public record OrderPublicCaseAdminView(
        String orderNo,
        boolean allowPublicCase,
        String publicSummary) {
}
