package com.xinghuo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendControlPageController {
    @GetMapping("/admin/frontend-controls")
    String page(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        return "admin-frontend-controls";
    }
}
