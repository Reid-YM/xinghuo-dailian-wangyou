package com.xinghuo.account.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConsolePageController {

    @GetMapping("/console")
    @RequireAccount
    public String console() {
        return "redirect:/account";
    }

    @GetMapping("/console/quotes")
    @RequireAccount
    public String quotes() {
        return "redirect:/account#quotes";
    }

    @GetMapping("/console/orders")
    @RequireAccount
    public String orders() {
        return "redirect:/account#orders";
    }

    @GetMapping("/console/security")
    @RequireAccount
    public String security() {
        return "redirect:/account/settings";
    }

    @GetMapping("/console/support")
    @RequireAccount
    public String support() {
        return "redirect:/contact";
    }
}
