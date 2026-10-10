package com.xinghuo.record.application;

/** Record-owned persistence boundary for unified public progress lookup. */
public interface RecordPort {
    RecordPublicView get(String publicNo);
}
