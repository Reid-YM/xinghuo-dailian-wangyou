package com.xinghuo.account.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Legacy /console compatibility only.
 *
 * <p>The user-facing information architecture is canonical under /account/**.
 * /console itself keeps a tiny client redirect page because URL fragments are not sent to the server.
 * Direct legacy paths are kept as server redirects so old bookmarks never fall through to 404 or admin pages.
 */
@Controller
public class ConsolePageController {

    @GetMapping("/console")
    @RequireAccount
    public String legacyConsole() {
        return "console";
    }

    @GetMapping("/console/overview")
    @RequireAccount
    public String legacyOverview() {
        return "redirect:/account";
    }

    @GetMapping("/console/profile")
    @RequireAccount
    public String legacyProfile() {
        return "redirect:/account/settings#profile";
    }

    @GetMapping({"/console/quotes", "/console/requests"})
    @RequireAccount
    public String legacyQuotes() {
        return "redirect:/account/requests";
    }

    @GetMapping("/console/orders")
    @RequireAccount
    public String legacyOrders() {
        return "redirect:/account/orders";
    }

    @GetMapping({"/console/progress", "/console/tools"})
    @RequireAccount
    public String legacyProgress() {
        return "redirect:/account/progress";
    }

    @GetMapping({"/console/security", "/console/settings"})
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
