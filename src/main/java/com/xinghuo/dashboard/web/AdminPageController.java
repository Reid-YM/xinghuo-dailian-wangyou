package com.xinghuo.dashboard.web;

import com.xinghuo.page.web.PublicPageModelSupport;
import lombok.Generated;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AdminPageController {
    private final PublicPageModelSupport pageModel;

    @ModelAttribute
    void fillCommon(Model model) {
        this.pageModel.fill(model);
    }

    @GetMapping(value={"/admin/login"})
    String login() {
        return "admin-login";
    }

    @GetMapping(value={"/admin"})
    String admin(Model model, CsrfToken csrf, Authentication auth) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", auth.getName());
        return "admin";
    }

    @Generated
    public AdminPageController(PublicPageModelSupport pageModel) {
        this.pageModel = pageModel;
    }
}
