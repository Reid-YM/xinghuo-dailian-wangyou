package com.xinghuo.order.application;

/** Framework-neutral command for creating an administrative order. */
public record OrderCreateCommand(
        String status,
        Integer progressPercent,
        String progressText,
        String customerNote,
        String internalNote,
        String reason) {
}
