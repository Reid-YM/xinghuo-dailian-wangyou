package com.xinghuo.controller;

import com.xinghuo.mobilevisual.application.MobileVisualConfigApplicationService;
import com.xinghuo.mobilevisual.application.MobileVisualConfigView;
import com.xinghuo.mobilevisual.web.MobileVisualConfigWebMapper;
import com.xinghuo.mobilevisual.web.MobileVisualConfigWebMapper.Response;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MobileVisualConfigController {
    private final MobileVisualConfigApplicationService service;
    private final AuditService audit;

    public MobileVisualConfigController(MobileVisualConfigApplicationService service, AuditService audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping({"/api/public/mobile-visual-config", "/api/admin/mobile-visual-config"})
    Response get() {
        return MobileVisualConfigWebMapper.toResponse(service.get());
    }

    @PatchMapping("/api/admin/mobile-visual-config")
    Response update(@Valid @RequestBody UpdateRequest body,
                    Authentication authentication,
                    HttpServletRequest request) {
        MobileVisualConfigView before = service.get();
        MobileVisualConfigView after = service.update(MobileVisualConfigWebMapper.toCommand(
                body.heroBackgroundMobileOpacity(),
                body.heroBackgroundMobileBlur()));
        String operator = authentication == null ? "admin" : authentication.getName();
        String reason = body.reason() == null || body.reason().isBlank()
                ? "后台手机背景参数保存"
                : body.reason().trim();
        audit.log("MOBILE_VISUAL_CONFIG_UPDATE", "SITE_SETTINGS", "mobile-background",
                operator, before, after, reason, request);
        return MobileVisualConfigWebMapper.toResponse(after);
    }

    public record UpdateRequest(
            @NotNull @Min(0) @Max(100) Integer heroBackgroundMobileOpacity,
            @NotNull @Min(0) @Max(24) Integer heroBackgroundMobileBlur,
            @Size(max = 500) String reason) {
    }
}
