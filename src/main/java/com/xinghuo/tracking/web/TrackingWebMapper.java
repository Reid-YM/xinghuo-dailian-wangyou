package com.xinghuo.tracking.web;

import com.xinghuo.tracking.application.TrackingCommand;

/** Maps the established tracking HTTP request into the application command. */
public final class TrackingWebMapper {
    private TrackingWebMapper() {
    }

    public static TrackingCommand toCommand(TrackRequest request, String ipHash) {
        return new TrackingCommand(
                request.eventName(), request.path(), request.refValue(), ipHash);
    }
}
