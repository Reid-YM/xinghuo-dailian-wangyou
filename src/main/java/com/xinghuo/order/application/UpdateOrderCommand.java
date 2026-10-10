package com.xinghuo.order.application;

/** Application-owned command for updating an administrative order. */
public record UpdateOrderCommand(
        Long version,
        String status,
        Integer progressPercent,
        String progressText,
        String customerNote,
        String internalNote,
        String reason) {
}
