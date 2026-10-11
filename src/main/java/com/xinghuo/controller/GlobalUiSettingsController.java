package com.xinghuo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import com.xinghuo.service.SiteSettingService;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/global-ui-settings")
public class GlobalUiSettingsController {
    private final SiteSettingService settings;
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;

    public GlobalUiSettingsController(SiteSettingService settings, AuditLogRepository audits, ObjectMapper mapper) {
        this.settings = settings;
        this.audits = audits;
        this.mapper = mapper;
    }

    @GetMapping
    Map<String, String> get() {
        return settings.globalUiSettings();
    }

    @PatchMapping
    Map<String, String> update(@RequestBody Map<String, Object> body,
                               Authentication authentication,
                               HttpServletRequest request) throws Exception {
        Map<String, String> before = settings.globalUiSettings();
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(body == null ? Map.of() : body);
        String reason = cleanReason(values.remove("reason"));
        Map<String, String> after = settings.updateGlobalUiSettings(values);

        audits.save(AuditLog.builder()
                .action("GLOBAL_UI_SETTINGS_UPDATE")
                .targetType("SITE_SETTINGS")
                .targetId("global-ui")
                .operatorName(authentication == null ? "admin" : authentication.getName())
                .beforeJson(mapper.writeValueAsString(before))
                .afterJson(mapper.writeValueAsString(after))
                .reason(reason)
                .ipAddress(RequestUtil.clientIp(request))
                .requestId(request.getHeader("X-Request-Id"))
                .createdAt(Instant.now())
                .build());
        return after;
    }

    private String cleanReason(Object value) {
        String reason = value == null ? "后台全局前端控制" : String.valueOf(value).trim();
        if (reason.isBlank()) reason = "后台全局前端控制";
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
