package com.xinghuo.slot.application;

/** Application-owned command for updating one home-page service slot. */
public record UpdateSlotCommand(
        Integer slotNo,
        Long serviceId,
        String mode,
        String reason) {
}
