package com.xinghuo.slot.web;

import com.xinghuo.slot.application.SlotView;
import com.xinghuo.slot.application.UpdateSlotCommand;
import java.util.List;

/** Maps established HTTP DTOs at the slot web boundary. */
public final class SlotWebMapper {
    private SlotWebMapper() {
    }

    public static UpdateSlotCommand toCommand(SlotUpdate request) {
        return new UpdateSlotCommand(
                request.slotNo(), request.serviceId(), request.mode(), request.reason());
    }

    public static SlotResponse toResponse(SlotView view) {
        return new SlotResponse(
                view.slotNo(), view.serviceId(), view.serviceName(), view.mode());
    }

    public static List<SlotResponse> toSlotResponses(List<SlotView> views) {
        return views.stream().map(SlotWebMapper::toResponse).toList();
    }
}
