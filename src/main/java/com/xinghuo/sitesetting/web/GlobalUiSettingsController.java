package com.xinghuo.sitesetting.web;

import com.xinghuo.sitesetting.application.GlobalUiSettingsAdminApplicationService;
import com.xinghuo.sitesetting.application.GlobalUiSettingsAuditContext;
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
@RequestMapping("/api/admin/global-ui-settings")
public class GlobalUiSettingsController {
    private final GlobalUiSettingsAdminApplicationService settings;

    public GlobalUiSettingsController(GlobalUiSettingsAdminApplicationService settings) {
        this.settings = settings;
    }

    @GetMapping
    Map<String, String> get() {
        return settings.get();
    }

    @PatchMapping
    Map<String, String> update(
            @RequestBody Map<String, Object> body,
            Authentication authentication,
            HttpServletRequest request) throws Exception {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>(body == null ? Map.of() : body);
        String reason = cleanReason(values.remove("reason"));
        GlobalUiSettingsAuditContext audit = new GlobalUiSettingsAuditContext(
                authentication == null ? "admin" : authentication.getName(),
                reason,
                RequestUtil.clientIp(request),
                request.getHeader("X-Request-Id"));
        return settings.update(SiteSettingWebMapper.toGlobalUiCommand(values), audit);
    }

    private String cleanReason(Object value) {
        String reason = value == null ? "后台全局前端控制" : String.valueOf(value).trim();
        if (reason.isBlank()) reason = "后台全局前端控制";
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}
