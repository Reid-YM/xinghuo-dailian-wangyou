package com.xinghuo.dashboard.application;

import org.springframework.stereotype.Service;

/** Application boundary for the administrative dashboard summary. */
@Service
public class DashboardApplicationService {
    private final DashboardPort port;

    public DashboardApplicationService(DashboardPort port) {
        this.port = port;
    }

    public DashboardView get() {
        return port.get();
    }
}
