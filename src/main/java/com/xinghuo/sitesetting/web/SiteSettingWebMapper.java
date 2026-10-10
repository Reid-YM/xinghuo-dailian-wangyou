package com.xinghuo.sitesetting.web;

import com.xinghuo.sitesetting.application.GlobalUiSettingsUpdateCommand;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SiteSettingWebMapper {
    private SiteSettingWebMapper() {
    }

    public static GlobalUiSettingsUpdateCommand toGlobalUiCommand(Map<String, Object> values) {
        if (values == null) {
            return new GlobalUiSettingsUpdateCommand(Map.of());
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            out.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return new GlobalUiSettingsUpdateCommand(out);
    }
}
