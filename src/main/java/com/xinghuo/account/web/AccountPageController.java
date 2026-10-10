package com.xinghuo.account.web;

import com.xinghuo.page.web.PublicPageModelSupport;
import lombok.Generated;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AccountPageController {
    private final PublicPageModelSupport pageModel;

    @ModelAttribute
    void fillCommon(Model model) {
        this.pageModel.fill(model);
    }

    @GetMapping(value={"/account/login"})
    String customerLogin() {
        return "account-login";
    }

    @GetMapping(value={"/account/register"})
    String customerRegister() {
        return "account-register";
    }

    @RequireAccount
    @GetMapping(value={"/account"})
    String customerAccount() {
        return "account";
    }

    @RequireAccount
    @GetMapping(value={"/account/settings"})
    String customerAccountSettings() {
        return "account-settings";
    }

    @RequireAccount
    @GetMapping(value={"/account/submit"})
    String customerAccountSubmit() {
        return "account-submit";
    }

    @GetMapping(value={"/account/reset"})
    String customerAccountReset() {
        return "account-reset";
    }

    @Generated
    public AccountPageController(PublicPageModelSupport pageModel) {
        this.pageModel = pageModel;
    }
}
