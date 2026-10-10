package com.xinghuo.mobilevisual.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.mobilevisual.application.MobileVisualConfigAuditContext;
import com.xinghuo.mobilevisual.application.MobileVisualConfigAuditPort;
import com.xinghuo.mobilevisual.application.MobileVisualConfigView;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class MobileVisualConfigAuditPersistenceAdapter implements MobileVisualConfigAuditPort {
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;

    public MobileVisualConfigAuditPersistenceAdapter(
            AuditLogRepository audits,
            ObjectMapper mapper) {
        this.audits = audits;
        this.mapper = mapper;
    }

    @Override
    public void record(
            MobileVisualConfigView before,
            MobileVisualConfigView after,
            MobileVisualConfigAuditContext context) {
        MobileVisualConfigAuditContext actual = context == null
                ? new MobileVisualConfigAuditContext("admin", "后台手机背景参数保存", null, null)
                : context;
        audits.save(AuditLog.builder()
                .action("MOBILE_VISUAL_CONFIG_UPDATE")
                .targetType("SITE_SETTINGS")
                .targetId("mobile-background")
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
        if (value == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }
}
