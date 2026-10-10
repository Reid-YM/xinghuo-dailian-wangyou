package com.xinghuo.frontendcontrol.application;

import java.util.Map;

public interface FrontendControlPort {
    Map<String, String> loadAll();
    void putAll(Map<String, String> values);
}
