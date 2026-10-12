package com.xinghuo.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 星维网络服务工作室新版后台入口。
 *
 * <p>V2C 在 V2B 混合权限基础上开始执行 ALL / DEPARTMENT / SELF 数据范围。
 * legacy ADMIN 永远全权，未配置星维角色的 OPERATOR 继续保持兼容行为；
 * 已配置星维角色的 OPERATOR 同时按权限点和数据范围执行。旧 /admin 边界保持原有行为。
 */
@Controller
public class XingweiAdminController {

    @GetMapping("/admin/xingwei")
    String console(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
        model.addAttribute("consoleVersion", "20261013-v2c");
        return "admin-xingwei";
    }

    @GetMapping("/api/admin/xingwei/health")
    @ResponseBody
    Map<String, Object> health(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("workspace", "星维网络服务工作室");
        result.put("version", "20261013-v2c");
        result.put("user", authentication == null ? "unknown" : authentication.getName());
        result.put("serverTime", OffsetDateTime.now().toString());
        return result;
    }
}
