package com.xinghuo.toolstation.application;

/** Persistence boundary for the tool-station JSON document. */
public interface ToolStationPort {
    String publicData();

    String save(String json);

    String reset();
}
