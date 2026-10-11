package com.xinghuo.siteconfig.web;

import com.xinghuo.siteconfig.application.SiteConfigFullUpdateCommand;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps the established admin HTTP DTO into a protocol-independent mutation command. */
public final class SiteConfigWriteWebMapper {
    private SiteConfigWriteWebMapper() {
    }

    public static SiteConfigFullUpdateCommand toCommand(SiteConfigUpdate request) {
        Map<String, Object> values = recordValues(request);
        Object reason = values.remove("reason");
        return new SiteConfigFullUpdateCommand(values, reason == null ? null : String.valueOf(reason));
    }

    private static Map<String, Object> recordValues(Record record) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            try {
                out.put(component.getName(), normalize(component.getAccessor().invoke(record)));
            }
            catch (ReflectiveOperationException e) {
                throw new IllegalStateException("无法转换站点配置请求", e);
            }
        }
        return out;
    }

    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Record record) {
            return recordValues(record);
        }
        if (value instanceof List<?> list) {
            ArrayList<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(normalize(item));
            }
            return out;
        }
        return value;
    }
}
