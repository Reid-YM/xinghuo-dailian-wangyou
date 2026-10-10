package com.xinghuo.mobilevisual.application;

import org.springframework.stereotype.Service;

@Service
public class MobileVisualConfigAdminApplicationService {
    private final MobileVisualConfigApplicationService configs;
    private final MobileVisualConfigAuditPort audit;

    public MobileVisualConfigAdminApplicationService(
            MobileVisualConfigApplicationService configs,
            MobileVisualConfigAuditPort audit) {
        this.configs = configs;
        this.audit = audit;
    }

    public MobileVisualConfigView get() {
        return configs.get();
    }

    public MobileVisualConfigView update(
            UpdateMobileVisualConfigCommand command,
            MobileVisualConfigAuditContext context) {
        MobileVisualConfigView before = configs.get();
        MobileVisualConfigView after = configs.update(command);
        audit.record(before, after, context);
        return after;
    }
}
