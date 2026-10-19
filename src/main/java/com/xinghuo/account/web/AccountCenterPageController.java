package com.xinghuo.account.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Canonical user-center pages and user-only compatibility redirects.
 *
 * <p>All user-facing destinations stay under {@code /account/**}. Legacy aliases only redirect to
 * canonical user pages; they must never point to {@code /admin/**}.
 */
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
    @GetMapping("/account/requests/{businessNo}")
    public String requestDetail() { return "account-requests"; }

    @RequireAccount
    @GetMapping("/account/orders/{businessNo}")
    public String orderDetail() { return "account-orders"; }

    @RequireAccount
    @GetMapping({"/account/favorites", "/account/submissions"})
    public String personalAssets() { return "account"; }

    @RequireAccount
    @GetMapping("/account/progress")
    public String progress() {
        return "account-progress";
    }

    @RequireAccount
    @GetMapping({"/account/quotes", "/account/demands"})
    public String legacyRequests() {
        return "redirect:/account/requests";
    }

    @RequireAccount
    @GetMapping({"/account/order", "/account/order-list"})
    public String legacyOrders() {
        return "redirect:/account/orders";
    }

    @RequireAccount
    @GetMapping({"/account/tools", "/account/tool-progress"})
    public String legacyProgress() {
        return "redirect:/account/progress";
    }

    @RequireAccount
    @GetMapping({"/account/profile", "/account/settings/profile"})
    public String legacyProfile() {
        return "redirect:/account/settings#profile";
    }

    @RequireAccount
    @GetMapping({"/account/contact", "/account/settings/contact"})
    public String legacyContact() {
        return "redirect:/account/settings#contact";
    }

    @RequireAccount
    @GetMapping({"/account/security", "/account/settings/security"})
    public String legacySecurity() {
        return "redirect:/account/settings#security";
    }

    @RequireAccount
    @GetMapping("/account/devices")
    public String legacyDevices() {
        return "redirect:/account/settings#devices";
    }

    @RequireAccount
    @GetMapping("/account/activity")
    public String legacyActivity() {
        return "redirect:/account/settings#activity";
    }
}
