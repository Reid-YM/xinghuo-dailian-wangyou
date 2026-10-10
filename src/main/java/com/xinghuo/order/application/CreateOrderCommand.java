package com.xinghuo.order.application;

/** Application-owned command for creating an administrative order. */
public record CreateOrderCommand(
        String status,
        Integer progressPercent,
        String progressText,
        String customerNote,
        String internalNote,
        String reason) {
}
