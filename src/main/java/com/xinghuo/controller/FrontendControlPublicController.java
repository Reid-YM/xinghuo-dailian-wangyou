package com.xinghuo.controller;

import com.xinghuo.service.FrontendControlService;
import com.xinghuo.web.DeviceModeResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/frontend-controls")
public class FrontendControlPublicController {
    private final FrontendControlService controls;
    private final DeviceModeResolver deviceModeResolver;

    public FrontendControlPublicController(FrontendControlService controls, DeviceModeResolver deviceModeResolver) {
        this.controls = controls;
        this.deviceModeResolver = deviceModeResolver;
    }

    @GetMapping
    Map<String, String> get(HttpServletRequest request) {
        return controls.effectiveSettings(deviceModeResolver.resolve(request));
    }
}
