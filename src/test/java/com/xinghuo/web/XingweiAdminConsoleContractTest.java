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

        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin/**\")"),
                "Xingwei console must reuse the existing protected admin boundary");
        assertTrue(controller.contains("@GetMapping(\"/admin/xingwei\")"));
        assertTrue(controller.contains("@GetMapping(\"/api/admin/xingwei/health\")"));
        assertTrue(accessController.contains("@GetMapping(\"/access\")"));
        assertTrue(!controller.contains("permitAll"), "the new console must not introduce anonymous access");
    }

    @Test
    void migrationKeepsLegacyAdminAndDoesNotIntroduceSecondAuthenticationStack() throws IOException {
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        String template = read("resources/templates/admin-xingwei.html");

        assertTrue(controller.contains("return \"admin-xingwei\""));
        assertTrue(template.contains("href=\"/admin\""), "legacy admin must remain available during migration");
        assertTrue(template.contains("action=\"/logout\""), "existing Spring Security logout must be reused");
        assertTrue(!template.contains("JWT"));
        assertTrue(!template.contains("localStorage"));
    }

    @Test
    void consoleHasResponsiveAssetsAndV2bPermissionBoundary() throws IOException {
        String template = read("resources/templates/admin-xingwei.html");
        String css = read("resources/static/assets/admin-xingwei.css");
        String js = read("resources/static/assets/admin-xingwei.js");

        assertTrue(template.contains("/assets/admin-xingwei.css?v=20261013v1"));
        assertTrue(template.contains("/assets/admin-xingwei.js?v=20261013v2b"));
        assertTrue(template.contains("/assets/admin-xingwei-org.js?v=20261013v2b"));
        assertTrue(template.contains("组织与权限"));
        assertTrue(template.contains("微信支付"));
        assertTrue(template.contains("沿用现有流水线"));
        assertTrue(template.contains("V2B 混合权限"));
        assertTrue(css.contains("@media(max-width:860px)"));
        assertTrue(css.contains("@media(max-width:590px)"));
        assertTrue(js.contains("/api/admin/xingwei/health"));
        assertTrue(js.contains("/api/admin/xingwei/access"));
        assertTrue(js.contains("xwModuleSearch"));
        assertTrue(js.contains("MODULE_PERMISSIONS"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
