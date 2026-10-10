package com.xinghuo.order.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.order.application.AdminOrderView;
import com.xinghuo.order.application.CreateOrderCommand;
import com.xinghuo.order.application.OrderPublicView;
import com.xinghuo.order.application.PublicCaseSettingsView;
import com.xinghuo.order.application.PublicCaseView;
import com.xinghuo.order.application.UpdateOrderCommand;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps HTTP-facing order DTOs to and from application-owned contracts. */
public final class OrderWebMapper {
    private OrderWebMapper() {
    }

    public static CreateOrderCommand toCommand(ApiDtos.CreateOrderRequest request) {
        return new CreateOrderCommand(
                request.status(),
                request.progressPercent(),
                request.progressText(),
                request.customerNote(),
                request.internalNote(),
                request.reason());
    }

    public static UpdateOrderCommand toCommand(ApiDtos.UpdateOrderRequest request) {
        return new UpdateOrderCommand(
                request.version(),
                request.status(),
                request.progressPercent(),
                request.progressText(),
                request.customerNote(),
                request.internalNote(),
                request.reason());
    }

    public static ApiDtos.OrderPublicResponse toResponse(OrderPublicView view) {
        return new ApiDtos.OrderPublicResponse(
                view.businessNo(),
                view.status(),
                view.progressPercent(),
                view.progressText(),
                view.updatedAt(),
                view.hasProgressImage(),
                view.customerNote());
    }

    public static ApiDtos.AdminOrderResponse toResponse(AdminOrderView view) {
        return new ApiDtos.AdminOrderResponse(
                view.id(),
                view.orderNo(),
                view.businessNo(),
                view.sourceQuoteId(),
                view.status(),
                view.progressPercent(),
                view.progressText(),
                view.hasProgressImage(),
                view.customerNote(),
                view.internalNote(),
                view.updatedAt(),
                view.version());
    }

    public static List<ApiDtos.AdminOrderResponse> toAdminResponses(List<AdminOrderView> views) {
        return views.stream().map(OrderWebMapper::toResponse).toList();
    }

    public static List<Map<String, Object>> toCaseResponses(List<PublicCaseView> views) {
        return views.stream().map(OrderWebMapper::toCaseResponse).toList();
    }

    public static Map<String, Object> toResponse(PublicCaseSettingsView view) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("orderNo", view.orderNo());
        response.put("allowPublicCase", view.allowPublicCase());
        response.put("publicSummary", view.publicSummary());
        return response;
    }

    private static Map<String, Object> toCaseResponse(PublicCaseView view) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("maskedNo", view.maskedNo());
        response.put("summary", view.summary());
        response.put("finishedAt", view.finishedAt());
        response.put("status", view.status());
        return response;
    }
}
