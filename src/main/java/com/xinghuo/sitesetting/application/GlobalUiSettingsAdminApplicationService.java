package com.xinghuo.sitesetting.application;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GlobalUiSettingsAdminApplicationService {
    private final SiteSettingApplicationService settings;
    private final GlobalUiSettingsAuditPort audit;

    public GlobalUiSettingsAdminApplicationService(
            SiteSettingApplicationService settings,
            GlobalUiSettingsAuditPort audit) {
        this.settings = settings;
        this.audit = audit;
    }

    public Map<String, String> get() {
        return settings.globalUiSettings();
    }

    public Map<String, String> update(
            GlobalUiSettingsUpdateCommand command,
            GlobalUiSettingsAuditContext context) throws Exception {
        Map<String, String> before = settings.globalUiSettings();
        Map<String, String> after = settings.updateGlobalUiSettings(command);
        audit.record(before, after, context);
        return after;
    }
}
