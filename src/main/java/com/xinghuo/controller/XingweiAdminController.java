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
 * V2E 增加独立询价/订单业务工作台，直接使用 V2D 后端范围接口。
 * V2F 增加派单后的员工“我的工作”入口，领取与履约更新继续复用同一业务归属。
 * V2G 增加企业微信通讯录只读页；只读取员工/部门，岗位继续复用 V63 正式词库。
 * V3A 增加服务项目与游戏资料只读工作台，直接复用现有 ServiceCatalogService / TaxonomyService。
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

    @GetMapping("/admin/xingwei/business")
    String businessWorkbench(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
        model.addAttribute("businessVersion", "20261013-v2e");
        return "admin-xingwei-business";
    }

    @GetMapping("/admin/xingwei/work")
    String employeeWorkbench(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
        model.addAttribute("workVersion", "20261013-v2f");
        return "admin-xingwei-work";
    }

    @GetMapping("/admin/xingwei/wecom")
    String weComDirectory(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
        model.addAttribute("weComVersion", "20261013-v2g");
        return "admin-xingwei-wecom";
    }

    @GetMapping("/admin/xingwei/catalog")
    String serviceCatalog(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
        model.addAttribute("catalogVersion", "20261013-v3a");
        return "admin-xingwei-catalog";
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
