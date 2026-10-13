package com.xinghuo.controller;

import com.xinghuo.security.XingweiPermissionGuard;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/xingwei")
public class XingweiPermissionController {
    private final XingweiPermissionGuard permissions;

    public XingweiPermissionController(XingweiPermissionGuard permissions) {
        this.permissions = permissions;
    }

    @GetMapping("/access")
    public Map<String, Object> access(Authentication authentication) {
        return permissions.asMap(authentication);
    }
}
