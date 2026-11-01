package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression guards for the website's restrictive Content-Security-Policy. */
class MiniWebsiteAdminCspContractTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static String read(String path) throws Exception {
        return Files.readString(ROOT.resolve(path));
    }

    @Test
    void miniAdminPageDoesNotRelyOnBlockedInlineJavascript() throws Exception {
        String html = read("templates/xingwei-mini-binding.html");
        String security = Files.readString(
                Path.of("src/main/java/com/xinghuo/security/SecurityConfig.java"));
        assertTrue(html.contains("src=\"/assets/mini-admin-binding.js\""));
        assertFalse(html.contains("<script th:inline="));
        assertFalse(html.contains("<script>"));
        assertTrue(security.contains("script-src 'self'"));
        assertFalse(html.contains("onclick="));
    }

    @Test
    void externalScriptKeepsCsrfAndAvoidsEmbeddingAdminSecrets() throws Exception {
        String script = read("static/assets/mini-admin-binding.js");
        assertTrue(script.contains("meta[name=\"_csrf\"]"));
        assertTrue(script.contains("meta[name=\"_csrf_header\"]"));
        assertTrue(script.contains("credentials: 'same-origin'"));
        assertTrue(script.contains("confirmButton.disabled = true"));
        assertTrue(script.contains("/api/admin/xingwei/mini-binding/confirm"));
        assertTrue(script.contains("/api/admin/xingwei/mini-binding/revoke"));
        assertFalse(script.contains("WECHAT_MINI_APP_SECRET"));
        assertFalse(script.contains("localStorage"));
    }

    @Test
    void mysqlSchemaAndTtlRemainMysqlCompatible() throws Exception {
        String sql = read("db/migration/V79__mini_admin_website_binding.sql");
        String bridge = Files.readString(
                Path.of("src/main/java/com/xinghuo/account/service/MiniWebsiteAdminBridgeService.java"));
        assertTrue(sql.contains("ENGINE=InnoDB"));
        assertTrue(sql.contains("DATETIME(6)"));
        assertFalse(sql.contains("TIMESTAMP WITH TIME ZONE"));
        assertTrue(bridge.contains("DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 5 MINUTE)"));
        assertFalse(bridge.contains("INTERVAL '5 minutes'"));
    }
}
