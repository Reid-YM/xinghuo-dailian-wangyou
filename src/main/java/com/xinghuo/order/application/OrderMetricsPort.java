package com.xinghuo.order.application;

/** Application-facing read contract for dashboard order metrics. */
public interface OrderMetricsPort {
    long countInProgress();

    long countCompleted();
}
