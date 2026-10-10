package com.xinghuo.order.application;

/** Framework-neutral command for updating an administrative order. */
public record OrderUpdateCommand(
        Long version,
        String status,
        Integer progressPercent,
        String progressText,
        String customerNote,
        String internalNote,
        String reason) {
}
