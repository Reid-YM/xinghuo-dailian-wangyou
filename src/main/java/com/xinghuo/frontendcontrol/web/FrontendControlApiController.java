package com.xinghuo.frontendcontrol.web;

import com.xinghuo.frontendcontrol.application.FrontendControlAdminApplicationService;
import com.xinghuo.frontendcontrol.application.FrontendControlAuditContext;
import com.xinghuo.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
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
    private final FrontendControlAdminApplicationService controls;

    public FrontendControlApiController(FrontendControlAdminApplicationService controls) {
        this.controls = controls;
    }

    @GetMapping
    Map<String, String> get() {
        return controls.settings();
    }

    @PatchMapping
    Map<String, String> update(
            @RequestBody Map<String, Object> body,
            Authentication authentication,
            HttpServletRequest request) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(body == null ? Map.of() : body);
        String reason = cleanReason(values.remove("reason"));
        FrontendControlAuditContext audit = new FrontendControlAuditContext(
                authentication == null ? "admin" : authentication.getName(),
                reason,
                RequestUtil.clientIp(request),
                request.getHeader("X-Request-Id"));
        return controls.update(FrontendControlWebMapper.toCommand(values), audit);
    }

    private String cleanReason(Object value) {
        String reason = value == null ? "后台前端控制中心" : String.valueOf(value).trim();
        if (reason.isBlank()) reason = "后台前端控制中心";
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
