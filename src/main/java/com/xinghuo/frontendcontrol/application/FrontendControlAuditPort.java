package com.xinghuo.frontendcontrol.application;

import java.util.Map;

public interface FrontendControlAuditPort {
    void record(
            Map<String, String> before,
            Map<String, String> after,
            FrontendControlAuditContext context);
}
