package com.xinghuo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FrontendControlPageController {
    @GetMapping("/admin/frontend-controls")
    String page(Model model, CsrfToken csrf, Authentication authentication,
                @RequestParam(defaultValue="0") String legacy, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (!"1".equals(legacy)) return "redirect:/admin/xingwei/frontend-controls";
        if (authentication == null || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
            throw new AccessDeniedException("旧版恢复入口仅限恢复管理员");
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication.getName());
        return "admin-frontend-controls";
    }
}
