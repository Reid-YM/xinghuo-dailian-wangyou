package com.xinghuo.sitesetting.application;

import java.util.Map;

public interface SiteSettingPort {
    Map<String, String> loadAll();
    String get(String key);
    void put(String key, String value);
    void putAll(Map<String, String> values);
}
