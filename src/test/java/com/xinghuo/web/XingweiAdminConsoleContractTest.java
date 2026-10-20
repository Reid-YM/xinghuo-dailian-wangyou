package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class XingweiAdminConsoleContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void consoleStaysInsideExistingAdminSecurityBoundary() throws IOException {
        String security = read("java/com/xinghuo/security/SecurityConfig.java");
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String accessController = read("java/com/xinghuo/controller/XingweiPermissionController.java");

        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin\", \"/api/admin/**\")"),
                "Xingwei console must reuse the existing protected admin boundary");
        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei\")"));
        assertTrue(controller.contains("@GetMapping(\"/api/admin/xingwei/health\")"));
        assertTrue(accessController.contains("@GetMapping(\"/access\")"));
        assertTrue(!controller.contains("permitAll"), "the new console must not introduce anonymous access");
    }

    @Test
    void finalWorkspaceRetiresLegacyLinksAndReusesAuthentication() throws IOException {
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei.html");

        assertTrue(controller.contains("return \"admin-xingwei\""));
        assertTrue(!template.contains("legacy=1"), "retired admin must not remain reachable from the native UI");
        assertTrue(template.contains("action=\"/logout\""), "existing Spring Security logout must be reused");
        assertTrue(!template.contains("JWT"));
        assertTrue(!template.contains("localStorage"));
    }

    @Test
    void consoleHasResponsiveAssetsAndRuntimeVerifiedPermissionBoundary() throws IOException {
        String template = read("resources/templates/admin-xingwei.html");
        String css = read("resources/static/assets/admin-xingwei.css");
        String js = read("resources/static/assets/admin-xingwei.js");

        assertTrue(template.contains("/assets/admin-xingwei.css?v=20261019cutover"));
        assertTrue(template.contains("/assets/admin-xingwei.js?v=20261019cutover"));
        assertTrue(template.contains("/assets/admin-xingwei-org.js?v=20261019cutover"));
        assertTrue(template.contains("组织与权限"));
        assertTrue(template.contains("微信支付"));
        assertTrue(template.contains("自动检查服务连接"));
        assertTrue(template.contains("员工数据范围"));
        assertTrue(read("resources/templates/fragments/admin-delivery-roadmap.html").contains("全部数据 / 本中心 / 仅本人"));
        assertTrue(css.contains("@media(max-width:860px)"));
        assertTrue(css.contains("@media(max-width:590px)"));
        assertTrue(js.contains("/api/admin/xingwei/health"));
        assertTrue(js.contains("/api/admin/xingwei/access"));
        assertTrue(js.contains("cache: 'no-store'"));
        assertTrue(js.contains("页面需要更新 · 请刷新"));
        assertTrue(js.contains("xwModuleSearch"));
        assertTrue(js.contains("MODULE_PERMISSIONS"));
        assertTrue(js.contains("canManageGlobalRbac"));
        assertTrue(js.contains("access.dataScope === 'ALL'"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
