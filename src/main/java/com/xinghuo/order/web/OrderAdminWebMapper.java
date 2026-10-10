package com.xinghuo.order.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.order.application.OrderAdminView;
import com.xinghuo.order.application.OrderCreateCommand;
import com.xinghuo.order.application.OrderPublicCaseAdminView;
import com.xinghuo.order.application.OrderUpdateCommand;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps established admin HTTP DTOs to Order-owned application contracts. */
public final class OrderAdminWebMapper {
    private OrderAdminWebMapper() {
    }

    public static OrderCreateCommand toCreateCommand(ApiDtos.CreateOrderRequest source) {
        return new OrderCreateCommand(
                source.status(),
                source.progressPercent(),
                source.progressText(),
                source.customerNote(),
                source.internalNote(),
                source.reason());
    }

    public static OrderUpdateCommand toUpdateCommand(ApiDtos.UpdateOrderRequest source) {
        return new OrderUpdateCommand(
                source.version(),
                source.status(),
                source.progressPercent(),
                source.progressText(),
                source.customerNote(),
                source.internalNote(),
                source.reason());
    }

    public static List<ApiDtos.AdminOrderResponse> toResponses(List<OrderAdminView> views) {
        return views.stream().map(OrderAdminWebMapper::toResponse).toList();
    }

    public static ApiDtos.AdminOrderResponse toResponse(OrderAdminView view) {
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

    public static Map<String, Object> toPublicCaseResponse(OrderPublicCaseAdminView view) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("orderNo", view.orderNo());
        out.put("allowPublicCase", view.allowPublicCase());
        out.put("publicSummary", view.publicSummary());
        return out;
    }
}
