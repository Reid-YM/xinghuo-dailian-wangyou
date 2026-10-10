package com.xinghuo.tracking.application;

import org.springframework.stereotype.Service;

@Service
public class TrackingAdminApplicationService {
    private final TrackingSummaryPort summaries;

    public TrackingAdminApplicationService(TrackingSummaryPort summaries) {
        this.summaries = summaries;
    }

    public TrackingSummaryView summary(int days) {
        int window = Math.max(1, Math.min(90, days));
        return summaries.summary(window);
    }
}
