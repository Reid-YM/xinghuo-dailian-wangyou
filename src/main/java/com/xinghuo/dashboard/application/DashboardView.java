package com.xinghuo.dashboard.application;

/** Application-owned administrative dashboard summary. */
public record DashboardView(
        long totalServices,
        long newQuotes,
        long inProgressOrders,
        long completedOrders,
        long trackingEvents,
        long newFeedback) {
}
