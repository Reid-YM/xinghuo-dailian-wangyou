package com.xinghuo.slot.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.slot.application.SlotPublicServiceView;
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

    public static ApiDtos.ServiceResponse toResponse(SlotPublicServiceView view) {
        return new ApiDtos.ServiceResponse(
                view.id(),
                view.slug(),
                view.gameCode(),
                view.name(),
                view.category(),
                view.shortDesc(),
                view.priceMode(),
                view.priceFixed(),
                view.priceMin(),
                view.priceMax(),
                view.durationText(),
                view.status(),
                view.coverImage());
    }

    public static List<ApiDtos.ServiceResponse> toPublicResponses(List<SlotPublicServiceView> views) {
        return views.stream().map(SlotWebMapper::toResponse).toList();
    }
}
