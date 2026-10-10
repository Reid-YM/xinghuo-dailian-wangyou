package com.xinghuo.tracking.application;

/** Application-facing tracking persistence contract. */
public interface TrackingPort {
    void track(TrackingCommand command);
}
