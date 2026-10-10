package com.xinghuo.slot.application;

/** Admin-facing hot-slot projection. */
public record SlotView(
        Integer slotNo,
        Long serviceId,
        String serviceName,
        String mode) {
}
