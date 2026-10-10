package com.xinghuo.frontendcontrol.web;

import com.xinghuo.frontendcontrol.application.FrontendControlUpdateCommand;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FrontendControlWebMapper {
    private FrontendControlWebMapper() {
    }

    public static FrontendControlUpdateCommand toCommand(Map<String, Object> values) {
        if (values == null) {
            return new FrontendControlUpdateCommand(Map.of());
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            out.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return new FrontendControlUpdateCommand(out);
    }
}
