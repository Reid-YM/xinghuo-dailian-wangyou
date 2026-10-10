package com.xinghuo.order.application;

import org.springframework.stereotype.Service;

@Service
public class OrderMetricsApplicationService {
    private final OrderMetricsPort port;

    public OrderMetricsApplicationService(OrderMetricsPort port) {
        this.port = port;
    }

    public long countInProgress() {
        return port.countInProgress();
    }

    public long countCompleted() {
        return port.countCompleted();
    }
}
