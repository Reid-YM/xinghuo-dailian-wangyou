package com.xinghuo.frontendcontrol.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.audit.infrastructure.AuditLog;
import com.xinghuo.audit.infrastructure.AuditLogRepository;
import com.xinghuo.frontendcontrol.application.FrontendControlAuditContext;
import com.xinghuo.frontendcontrol.application.FrontendControlAuditPort;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FrontendControlAuditPersistenceAdapter implements FrontendControlAuditPort {
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;

    public FrontendControlAuditPersistenceAdapter(
            AuditLogRepository audits,
            ObjectMapper mapper) {
        this.audits = audits;
        this.mapper = mapper;
    }

    @Override
    public void record(
            Map<String, String> before,
            Map<String, String> after,
            FrontendControlAuditContext context) {
        FrontendControlAuditContext actual = context == null
                ? new FrontendControlAuditContext("admin", "后台前端控制中心", null, null)
                : context;
        audits.save(AuditLog.builder()
                .action("FRONTEND_CONTROLS_UPDATE")
                .targetType("SITE_SETTINGS")
                .targetId("frontend-controls")
                .operatorName(actual.operator())
                .beforeJson(json(before))
                .afterJson(json(after))
                .reason(actual.reason())
                .ipAddress(actual.ipAddress())
                .requestId(actual.requestId())
                .createdAt(Instant.now())
                .build());
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }
}
