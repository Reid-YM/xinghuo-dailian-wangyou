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

    public static UpdateSlotCommand toCommand(ApiDtos.SlotUpdate request) {
        return new UpdateSlotCommand(
                request.slotNo(), request.serviceId(), request.mode(), request.reason());
    }

    public static ApiDtos.SlotResponse toResponse(SlotView view) {
        return new ApiDtos.SlotResponse(
                view.slotNo(), view.serviceId(), view.serviceName(), view.mode());
    }

    public static List<ApiDtos.SlotResponse> toSlotResponses(List<SlotView> views) {
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
