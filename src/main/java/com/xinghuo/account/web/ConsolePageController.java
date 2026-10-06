package com.xinghuo.account.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConsolePageController {

    @GetMapping({"/console", "/console/orders", "/console/quotes", "/console/security", "/console/support"})
    @RequireAccount
    public String console() {
        return "console";
    }
}
