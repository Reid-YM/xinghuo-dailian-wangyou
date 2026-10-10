package com.xinghuo.tracking.application;

import org.springframework.stereotype.Service;

/** Application boundary for public interaction tracking. */
@Service
public class TrackingApplicationService {
    private final TrackingPort port;

    public TrackingApplicationService(TrackingPort port) {
        this.port = port;
    }

    public void track(TrackingCommand command) {
        port.track(command);
    }

    public long totalCount() {
        return port.totalCount();
    }
}
