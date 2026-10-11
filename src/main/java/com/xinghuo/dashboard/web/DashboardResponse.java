package com.xinghuo.dashboard.web;

public record DashboardResponse(
        long totalServices,
        long newQuotes,
        long inProgressOrders,
        long completedOrders,
        long trackingEvents,
        long newFeedback) {
}
