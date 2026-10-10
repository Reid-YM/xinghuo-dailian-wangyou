package com.xinghuo.tracking.application;

import java.util.List;

public record TrackingSummaryView(
        int days,
        long total,
        long recent,
        String lastEventAt,
        List<EventSummary> events,
        List<FunnelStep> funnel,
        List<TrendPoint> trend) {

    public record EventSummary(
            String event,
            long total,
            long recent,
            String lastAt) {
    }

    public record FunnelStep(
            String event,
            String label,
            long count,
            double rate) {
    }

    public record TrendPoint(
            String date,
            long count) {
    }
}
