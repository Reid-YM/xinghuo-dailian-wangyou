package com.xinghuo.sitesetting.application;

import java.util.Map;

public interface GlobalUiSettingsAuditPort {
    void record(
            Map<String, String> before,
            Map<String, String> after,
            GlobalUiSettingsAuditContext context) throws Exception;
}
