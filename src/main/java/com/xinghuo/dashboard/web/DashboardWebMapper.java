package com.xinghuo.dashboard.web;

import com.xinghuo.dashboard.application.DashboardView;
import com.xinghuo.dto.ApiDtos;

/** Maps the dashboard application projection to the established admin API contract. */
public final class DashboardWebMapper {
    private DashboardWebMapper() {
    }

    public static ApiDtos.DashboardResponse toResponse(DashboardView view) {
        return new ApiDtos.DashboardResponse(
                view.totalServices(),
                view.newQuotes(),
                view.inProgressOrders(),
                view.completedOrders(),
                view.trackingEvents(),
                view.newFeedback());
    }
}
