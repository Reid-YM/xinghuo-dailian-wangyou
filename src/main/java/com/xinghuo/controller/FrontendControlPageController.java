package com.xinghuo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendControlPageController {
    @GetMapping("/admin/frontend-controls")
    String page(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return "redirect:/admin/xingwei/frontend-controls";
    }
}
