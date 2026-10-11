package com.xinghuo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.model.AuditLog;
import com.xinghuo.repository.AuditLogRepository;
import com.xinghuo.service.FrontendControlService;
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
@RequestMapping("/api/admin/frontend-controls")
public class FrontendControlApiController {
    private final FrontendControlService controls;
    private final AuditLogRepository audits;
    private final ObjectMapper mapper;

    public FrontendControlApiController(FrontendControlService controls, AuditLogRepository audits, ObjectMapper mapper) {
        this.controls = controls;
        this.audits = audits;
        this.mapper = mapper;
    }

    @GetMapping
    Map<String, String> get() {
        return controls.settings();
    }

    @PatchMapping
    Map<String, String> update(@RequestBody Map<String, Object> body,
                               Authentication authentication,
                               HttpServletRequest request) throws Exception {
        Map<String, String> before = controls.settings();
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(body == null ? Map.of() : body);
        String reason = cleanReason(values.remove("reason"));
        Map<String, String> after = controls.update(values);
        audits.save(AuditLog.builder()
                .action("FRONTEND_CONTROLS_UPDATE")
                .targetType("SITE_SETTINGS")
                .targetId("frontend-controls")
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
        String reason = value == null ? "后台前端控制中心" : String.valueOf(value).trim();
        if (reason.isBlank()) reason = "后台前端控制中心";
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
