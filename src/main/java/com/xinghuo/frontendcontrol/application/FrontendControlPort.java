package com.xinghuo.frontendcontrol.application;

import java.util.Map;

public interface FrontendControlPort {
    Map<String, String> loadAll();
    void put(String key, String value);
}
