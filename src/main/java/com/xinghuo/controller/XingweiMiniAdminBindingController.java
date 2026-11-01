package com.xinghuo.controller;

import com.xinghuo.account.service.MiniWebsiteAdminBridgeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/** Browser-side confirmation inherits website session authentication AND CSRF. */
@Controller
public class XingweiMiniAdminBindingController {
    private final MiniWebsiteAdminBridgeService bridge;
    public XingweiMiniAdminBindingController(MiniWebsiteAdminBridgeService bridge) {
        this.bridge = bridge;
    }
    public record Confirm(@Pattern(regexp="[0-9]{8}") String code) {}

    @GetMapping("/admin/xingwei/mini-binding")
    public String page() {
        return "xingwei-mini-binding";
    }

    @PostMapping("/api/admin/xingwei/mini-binding/confirm")
    @ResponseBody
    public Map<String,String> confirm(@Valid @RequestBody Confirm body,
            Authentication identity, HttpServletRequest request) {
        bridge.confirm(body.code(), identity, request);
        return Map.of("message", "网站管理员与微信小程序已绑定");
    }

    @PostMapping("/api/admin/xingwei/mini-binding/revoke")
    @ResponseBody
    public Map<String,String> revoke(Authentication identity, HttpServletRequest request) {
        bridge.revoke(identity, request);
        return Map.of("message", "已解除微信小程序管理员绑定");
    }
}