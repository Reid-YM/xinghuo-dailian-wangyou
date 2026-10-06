package com.xinghuo.account.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Legacy /console compatibility only.
 *
 * <p>The user-facing information architecture is canonical under /account/**.
 * /console itself keeps a tiny client redirect page because URL fragments are not sent to the server.
 */
@Controller
public class ConsolePageController {

    @GetMapping("/console")
    @RequireAccount
    public String legacyConsole() {
        return "console";
    }

    @GetMapping("/console/quotes")
    @RequireAccount
    public String legacyQuotes() {
        return "redirect:/account/requests";
    }

    @GetMapping("/console/orders")
    @RequireAccount
    public String legacyOrders() {
        return "redirect:/account/orders";
    }

    @GetMapping("/console/security")
    @RequireAccount
    public String legacySecurity() {
        return "redirect:/account/settings#security";
    }

    @GetMapping("/console/support")
    @RequireAccount
    public String legacySupport() {
        return "redirect:/contact";
    }
}
