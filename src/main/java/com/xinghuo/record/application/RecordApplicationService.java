package com.xinghuo.record.application;

import org.springframework.stereotype.Service;

/** Application boundary for unified public record lookup. */
@Service
public class RecordApplicationService {
    private final RecordPort port;

    public RecordApplicationService(RecordPort port) {
        this.port = port;
    }

    public RecordPublicView get(String publicNo) {
        return port.get(publicNo);
    }
}
