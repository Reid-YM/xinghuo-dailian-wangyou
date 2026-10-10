package com.xinghuo.order.application;

import java.util.UUID;

/** Application-owned command for creating an order from a quote snapshot. */
public record OrderFromQuoteCommand(
        UUID quoteId,
        String businessNo,
        Long customerAccountId,
        String game,
        String demand,
        String currentProgress,
        String platform,
        String expectedTime,
        String reason) {
}
