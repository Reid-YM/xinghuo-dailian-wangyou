package com.xinghuo.controller;

import com.xinghuo.service.FrontendControlService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/frontend-controls")
public class FrontendControlPublicController {
    private final FrontendControlService controls;

    public FrontendControlPublicController(FrontendControlService controls) {
        this.controls = controls;
    }

    @GetMapping
    Map<String, String> get() {
        return controls.settings();
    }
}
