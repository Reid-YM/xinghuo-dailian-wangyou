package com.xinghuo.siteconfig.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistence- and protocol-independent full SiteConfig update payload. */
public record SiteConfigFullUpdateCommand(Map<String, Object> values, String reason) {
    public SiteConfigFullUpdateCommand {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
