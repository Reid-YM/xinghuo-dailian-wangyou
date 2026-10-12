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
 * <p>V2A 在 V1 并行后台壳基础上接入固定组织架构、正式岗位词库、员工档案与影子 RBAC。
 * 现有 /admin、Spring Security ADMIN/OPERATOR 授权、JPA/Flyway 和生产发布链路保持不变。
 */
@Controller
public class XingweiAdminController {

    @GetMapping("/admin/xingwei")
    String console(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
        model.addAttribute("consoleVersion", "20261013-v2a");
        return "admin-xingwei";
    }

    @GetMapping("/api/admin/xingwei/health")
    @ResponseBody
    Map<String, Object> health(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("workspace", "星维网络服务工作室");
        result.put("version", "20261013-v2a");
        result.put("user", authentication == null ? "unknown" : authentication.getName());
        result.put("serverTime", OffsetDateTime.now().toString());
        return result;
    }
}
