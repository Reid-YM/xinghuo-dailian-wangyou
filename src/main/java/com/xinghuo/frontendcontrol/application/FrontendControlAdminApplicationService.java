package com.xinghuo.frontendcontrol.application;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FrontendControlAdminApplicationService {
    private final FrontendControlApplicationService controls;
    private final FrontendControlAuditPort audit;

    public FrontendControlAdminApplicationService(
            FrontendControlApplicationService controls,
            FrontendControlAuditPort audit) {
        this.controls = controls;
        this.audit = audit;
    }

    public Map<String, String> settings() {
        return controls.settings();
    }

    public Map<String, String> update(
            FrontendControlUpdateCommand command,
            FrontendControlAuditContext context) {
        Map<String, String> before = controls.settings();
        Map<String, String> after = controls.update(command);
        audit.record(before, after, context);
        return after;
    }
}
