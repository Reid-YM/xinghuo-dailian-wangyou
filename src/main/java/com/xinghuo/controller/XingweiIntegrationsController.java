package com.xinghuo.controller;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.XingweiIntegrationConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Admin-only integration readiness; credentials are configured on the server. */
@Controller
public class XingweiIntegrationsController {
    private final XingweiIntegrationConfigService config;
    private final XingweiPermissionGuard permissions;

    public XingweiIntegrationsController(XingweiIntegrationConfigService config,
                                         XingweiPermissionGuard permissions) {
        this.config = config;
        this.permissions = permissions;
    }

    @GetMapping("/admin/xingwei/integrations")
    @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication, 'employee:view', 'payment:view')")
    String integrations(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication.getName());
        if (permissions.has(authentication, "employee:view")) {
            model.addAttribute("weCom", config.weCom());
        }
        if (permissions.has(authentication, "payment:view")) {
            model.addAttribute("payment", config.payment());
        }
        return "admin-xingwei-integrations";
    }
}
