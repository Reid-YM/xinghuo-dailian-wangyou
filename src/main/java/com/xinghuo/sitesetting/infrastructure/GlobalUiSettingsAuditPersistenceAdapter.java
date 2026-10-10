package com.xinghuo.sitesetting.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import com.xinghuo.sitesetting.application.GlobalUiSettingsAuditContext;
import com.xinghuo.sitesetting.application.GlobalUiSettingsAuditPort;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GlobalUiSettingsAuditPersistenceAdapter implements GlobalUiSettingsAuditPort {
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;

    public GlobalUiSettingsAuditPersistenceAdapter(
            AuditLogRepository audits,
            ObjectMapper mapper) {
        this.audits = audits;
        this.mapper = mapper;
    }

    @Override
    public void record(
            Map<String, String> before,
            Map<String, String> after,
            GlobalUiSettingsAuditContext context) throws Exception {
        GlobalUiSettingsAuditContext actual = context == null
                ? new GlobalUiSettingsAuditContext("admin", "后台全局前端控制", null, null)
                : context;
        audits.save(AuditLog.builder()
                .action("GLOBAL_UI_SETTINGS_UPDATE")
                .targetType("SITE_SETTINGS")
                .targetId("global-ui")
                .operatorName(actual.operator())
                .beforeJson(mapper.writeValueAsString(before))
                .afterJson(mapper.writeValueAsString(after))
                .reason(actual.reason())
                .ipAddress(actual.ipAddress())
                .requestId(actual.requestId())
                .createdAt(Instant.now())
                .build());
    }
}
