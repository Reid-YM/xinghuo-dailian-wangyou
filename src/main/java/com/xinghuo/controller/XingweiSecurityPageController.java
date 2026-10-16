package com.xinghuo.controller;

import com.xinghuo.security.XingweiPermissionGuard;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class XingweiSecurityPageController {
    private final XingweiPermissionGuard permissions;
    public XingweiSecurityPageController(XingweiPermissionGuard permissions) { this.permissions = permissions; }
    @ModelAttribute void privateResponse(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}

    @GetMapping("/admin/xingwei/security")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'employee:view')")
    String page(Model model, CsrfToken csrf, Authentication actor) {
        var access = permissions.resolve(actor);
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", actor.getName());
        model.addAttribute("accessMode", access.mode());
        model.addAttribute("dataScope", access.dataScope());
        return "admin-xingwei-security";
    }
}
