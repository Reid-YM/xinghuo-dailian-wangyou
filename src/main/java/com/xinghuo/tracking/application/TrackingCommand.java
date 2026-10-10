package com.xinghuo.tracking.application;

/** Application-owned command for recording one allowed public tracking event. */
public record TrackingCommand(
        String eventName,
        String path,
        String refValue,
        String ipHash) {
}
