package com.xinghuo.account.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Canonical user-center pages. Keeps business APIs unchanged while separating user-facing responsibilities. */
@Controller
public class AccountCenterPageController {

    @RequireAccount
    @GetMapping("/account/requests")
    public String requests() {
        return "account-requests";
    }

    @RequireAccount
    @GetMapping("/account/orders")
    public String orders() {
        return "account-orders";
    }

    @RequireAccount
    @GetMapping("/account/progress")
    public String progress() {
        return "account-progress";
    }
}
