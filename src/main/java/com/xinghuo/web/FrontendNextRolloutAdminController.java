package com.xinghuo.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Frontend Next staged rollout control.
 *
 * <p>This page never changes routes by itself. It only exposes audited, allow-listed
 * rollout settings already handled by the existing global UI settings endpoint.</p>
 */
@Controller
public class FrontendNextRolloutAdminController {

    @GetMapping("/admin/xingwei/frontend-next")
    public String rollout(Model model, CsrfToken csrf) {
        model.addAttribute("csrf", csrf);
        return "admin-xingwei-frontend-next";
    }
}
