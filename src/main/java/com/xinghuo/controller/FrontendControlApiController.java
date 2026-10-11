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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    Map<String, String> get(@RequestParam(defaultValue = "global") String scope) {
        return controls.settings(scope);
    }

    @PatchMapping
    Map<String, String> update(@RequestParam(defaultValue = "global") String scope,
                               @RequestBody Map<String, Object> body,
                               Authentication authentication,
                               HttpServletRequest request) throws Exception {
        Map<String, String> before = controls.settings(scope);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(body == null ? Map.of() : body);
        String reason = cleanReason(values.remove("reason"));
        Map<String, String> after = controls.update(scope, values);
        audit("FRONTEND_CONTROLS_UPDATE", scope, before, after, reason, authentication, request);
        return after;
    }

    @DeleteMapping
    Map<String, String> reset(@RequestParam String scope,
                              Authentication authentication,
                              HttpServletRequest request) throws Exception {
        Map<String, String> before = controls.settings(scope);
        Map<String, String> after = controls.reset(scope);
        audit("FRONTEND_CONTROLS_RESET", scope, before, after,
                "恢复继承全局前端配置", authentication, request);
        return after;
    }

    private void audit(String action,
                       String scope,
                       Map<String, String> before,
                       Map<String, String> after,
                       String reason,
                       Authentication authentication,
                       HttpServletRequest request) throws Exception {
        audits.save(AuditLog.builder()
                .action(action)
                .targetType("SITE_SETTINGS")
                .targetId("frontend-controls:" + scope)
                .operatorName(authentication == null ? "admin" : authentication.getName())
                .beforeJson(mapper.writeValueAsString(before))
                .afterJson(mapper.writeValueAsString(after))
                .reason(reason)
                .ipAddress(RequestUtil.clientIp(request))
                .requestId(request.getHeader("X-Request-Id"))
                .createdAt(Instant.now())
                .build());
    }

    private String cleanReason(Object value) {
        String reason = value == null ? "后台前端控制中心" : String.valueOf(value).trim();
        if (reason.isBlank()) reason = "后台前端控制中心";
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
