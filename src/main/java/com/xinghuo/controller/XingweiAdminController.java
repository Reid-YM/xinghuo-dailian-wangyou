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
 * <p>第一阶段只提供独立后台壳与现有后台能力导航，不替换现有 /admin，
 * 不引入第二套认证，也不修改现有数据库结构。后续业务模块逐步迁移到本入口。
 */
@Controller
public class XingweiAdminController {

    @GetMapping("/admin/xingwei")
    String console(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
        model.addAttribute("consoleVersion", "20261013-v1");
        return "admin-xingwei";
    }

    @GetMapping("/api/admin/xingwei/health")
    @ResponseBody
    Map<String, Object> health(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("workspace", "星维网络服务工作室");
        result.put("version", "20261013-v1");
        result.put("user", authentication == null ? "unknown" : authentication.getName());
        result.put("serverTime", OffsetDateTime.now().toString());
        return result;
    }
}
