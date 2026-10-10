package com.xinghuo.toolstation.application;

import org.springframework.stereotype.Service;

/** Application boundary for public and administrative tool-station data flows. */
@Service
public class ToolStationApplicationService {
    private final ToolStationPort port;

    public ToolStationApplicationService(ToolStationPort port) {
        this.port = port;
    }

    public String publicData() {
        return port.publicData();
    }

    public String save(String json) {
        return port.save(json);
    }

    public String reset() {
        return port.reset();
    }
}
