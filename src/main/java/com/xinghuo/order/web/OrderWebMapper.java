package com.xinghuo.order.web;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.order.application.OrderPublicView;
import com.xinghuo.order.application.PublicCaseView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps application-owned order views to the established public API contract. */
public final class OrderWebMapper {
    private OrderWebMapper() {
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

    public static List<Map<String, Object>> toCaseResponses(List<PublicCaseView> views) {
        return views.stream().map(OrderWebMapper::toCaseResponse).toList();
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
