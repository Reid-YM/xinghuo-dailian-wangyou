package com.xinghuo.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
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
 * <p>版本号和首页阶段状态集中维护在本类，避免功能已更新但后台仍显示旧阶段。
 * 每个可交付阶段同时更新 RELEASE_HISTORY、对应模块版本、页面阶段说明与状态契约测试。
 * V3K 把版本/阶段/更新记录一致性检查下沉到 CI；V3L 再由浏览器核对页面声明与实际运行后端版本。
 */
@Controller
public class XingweiAdminController {
    static final String CONSOLE_VERSION = "20261014-v3l";
    static final String CONSOLE_STAGE = "V3L · STABLE PRODUCTION";
    static final String BUSINESS_VERSION = "20261013-v2e";
    static final String WORK_VERSION = "20261013-v2f";
    static final String WECOM_VERSION = "20261013-v2g";
    static final String CATALOG_VERSION = "20261014-v3f";
    static final String TAXONOMY_VERSION = "20261014-v3i";

    private static final List<ReleaseItem> RELEASE_HISTORY = List.of(
            new ReleaseItem("2026-10-14", "V3L", "已上线",
                    "后台运行版本自校验",
                    "新版后台会把页面渲染的版本/阶段与 /api/admin/xingwei/health 返回的实际运行版本/阶段逐项核对；一致时显示运行版本已核对，浏览器缓存或异常发布造成不一致时立即显示版本漂移告警，并禁止再把单纯 HTTP 200 误判为完整健康。"),
            new ReleaseItem("2026-10-14", "V3K", "已上线",
                    "版本与更新记录自动巡检",
                    "新增 CI 版本真值校验：自动读取控制台版本/阶段并核对后台首页、更新记录、当前 ReleaseItem、V3H 待验收状态和已启用模块的 planned 标记；任一处遗漏同步都会阻断 PR/主线 CI。"),
            new ReleaseItem("2026-10-14", "V3J", "已上线",
                    "新版后台生产状态一致性与版本真值守卫",
                    "将已部署的 V3I 从发布候选同步为生产已上线；微信支付保留待真机验收状态，生产发布流水线标记为已启用。首页阶段标签改为集中版本真源，并由契约测试阻止已上线能力再次显示为候选或规划中。"),
            new ReleaseItem("2026-10-14", "V3I", "已上线",
                    "游戏/分类前台可见性确认与过期预览防护",
                    "启停前显示当前与操作后的前台服务数量、父级游戏阻断和风险提示；提交携带影响快照 token，预览后数据发生变化会拒绝旧确认并要求重新查看。Java/Maven 与 Chromium/WebKit 自动化验证通过，并已通过零停机生产发布流程上线。"),
            new ReleaseItem("2026-10-14", "V3H", "待真机验收",
                    "小程序微信支付闭环",
                    "JSAPI/小程序支付、直接 openid 绑定、Native/小程序通道互斥、支付状态服务端核验已完成；PR #384 保持未合并，待 AppID/商户绑定和小额真机联调。"),
            new ReleaseItem("2026-10-14", "V3G", "已上线",
                    "星维网络小程序扫码登录",
                    "网站生成二维码，小程序 wx.scanCode + wx.login 确认身份，原浏览器二次确认后签发会话；小程序源码已纳入 CI 并自动打包。"),
            new ReleaseItem("2026-10-14", "V3F", "已上线",
                    "游戏与分类受控新增及资料编辑",
                    "新增游戏/分类默认停用；资料编辑不改变 code、归属和启停状态；启停继续走影响预览、原因和审计。"),
            new ReleaseItem("2026-10-13", "V3E", "已上线",
                    "游戏与分类影响预览及受控启停",
                    "启停前展示分类、服务和公开候选影响，要求填写原因并立即刷新公开可见性缓存。"),
            new ReleaseItem("2026-10-13", "V3A-V3D", "已上线",
                    "新版服务目录工作台",
                    "完成服务目录只读工作台、基础编辑、服务新增、恢复上架发布检查，并继续复用现有业务真源。"),
            new ReleaseItem("2026-10-13", "V2E-V2G", "已上线",
                    "业务工作台、员工工作台与企业微信目录",
                    "询价/订单进入新版数据范围工作台，员工领取与履约入口上线，企业微信通讯录只读接入。"),
            new ReleaseItem("2026-10-13", "V2A-V2C", "已上线",
                    "组织、RBAC 与数据范围底座",
                    "原控制台版本 20261013-v2c：七大中心、155 岗位、角色权限与 ALL / DEPARTMENT / SELF 数据范围成为新版后台授权基础。")
    );

    @GetMapping("/admin/xingwei")
    String console(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("consoleVersion", CONSOLE_VERSION);
        model.addAttribute("consoleStage", CONSOLE_STAGE);
        model.addAttribute("releaseHistory", RELEASE_HISTORY);
        return "admin-xingwei";
    }

    @GetMapping("/admin/xingwei/releases")
    String releases(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("consoleVersion", CONSOLE_VERSION);
        model.addAttribute("releaseHistory", RELEASE_HISTORY);
        return "admin-xingwei-releases";
    }

    @GetMapping("/admin/xingwei/business")
    String businessWorkbench(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("businessVersion", BUSINESS_VERSION);
        return "admin-xingwei-business";
    }

    @GetMapping("/admin/xingwei/work")
    String employeeWorkbench(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("workVersion", WORK_VERSION);
        return "admin-xingwei-work";
    }

    @GetMapping("/admin/xingwei/wecom")
    String weComDirectory(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("weComVersion", WECOM_VERSION);
        return "admin-xingwei-wecom";
    }

    @GetMapping("/admin/xingwei/catalog")
    String serviceCatalog(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("catalogVersion", CATALOG_VERSION);
        return "admin-xingwei-catalog";
    }

    @GetMapping("/admin/xingwei/taxonomy")
    String taxonomyControl(Model model, CsrfToken csrf, Authentication authentication) {
        common(model, csrf, authentication);
        model.addAttribute("taxonomyVersion", TAXONOMY_VERSION);
        return "admin-xingwei-taxonomy";
    }

    @GetMapping("/api/admin/xingwei/health")
    @ResponseBody
    Map<String, Object> health(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("workspace", "星维网络服务工作室");
        result.put("version", CONSOLE_VERSION);
        result.put("stage", CONSOLE_STAGE);
        result.put("user", authentication == null ? "unknown" : authentication.getName());
        result.put("serverTime", OffsetDateTime.now().toString());
        return result;
    }

    private void common(Model model, CsrfToken csrf, Authentication authentication) {
        model.addAttribute("csrf", csrf);
        model.addAttribute("username", authentication == null ? "admin" : authentication.getName());
        model.addAttribute("workspaceName", "星维网络服务工作室");
    }

    public record ReleaseItem(String date, String version, String status, String title, String detail) {}
}
