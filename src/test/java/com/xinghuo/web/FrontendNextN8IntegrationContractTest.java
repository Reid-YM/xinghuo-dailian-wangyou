package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN8IntegrationContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void runtimeChainIsCompleteAndN8RunsLast() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        String[] runtimes = {
                "N2_RUNTIME", "N3_RUNTIME", "N4_RUNTIME", "N4_MINI_RUNTIME",
                "N5_RUNTIME", "N5_ACTIONS_RUNTIME", "N6_RUNTIME", "N7_RUNTIME", "N8_RUNTIME"
        };
        int previous = -1;
        for (String runtime : runtimes) {
            int index = controller.indexOf("if (!html.contains(" + runtime + "))");
            assertTrue(index > previous, runtime + " must be injected after the previous Frontend Next runtime");
            previous = index;
        }
        assertTrue(controller.contains("/next/n8-integration.js"));
        assertTrue(controller.contains("\"/next\""));
        assertTrue(controller.contains("\"/next/services\""));
        assertTrue(controller.contains("\"/next/quote\""));
        assertTrue(controller.contains("\"/next/account\""));
    }

    @Test
    void replacementGateKeepsLegacyFallbacksAndAdminProtection() throws Exception {
        String security = read("java/com/xinghuo/security/SecurityConfig.java");

        assertTrue(security.contains("\"/next\", \"/next/**\""));
        assertTrue(security.contains("\"/account\", \"/account/login\", \"/account/register\", \"/account/reset\""));
        assertTrue(security.contains(".requestMatchers(PERMIT_ALL).permitAll()"));
        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin/**\")"));
        assertTrue(security.contains(".hasAnyRole(\"ADMIN\", \"OPERATOR\")"));
    }

    @Test
    void N8FixesStalePreviewTruthAndAddsNonBlockingResilience() throws Exception {
        String runtime = Files.readString(NEXT.resolve("n8-integration.js"));

        assertTrue(runtime.contains("N8 · TOTAL INTEGRATION"));
        assertTrue(runtime.contains("不代表当前账号真实订单"));
        assertTrue(runtime.contains("这是新版预览，可通过页面中的旧版入口返回。"));
        assertTrue(runtime.contains("prefers-reduced-motion:reduce"));
        assertTrue(runtime.contains("当前网络已断开"));
        assertTrue(runtime.contains("aria-live"));
        assertTrue(runtime.contains("XHFrontendNextN2"));
        assertTrue(runtime.contains("XHFrontendNextN3"));
        assertTrue(runtime.contains("XHFrontendNextN4"));
        assertTrue(runtime.contains("XHFrontendNextN4Mini"));
        assertTrue(runtime.contains("XHFrontendNextN5"));
        assertTrue(runtime.contains("XHFrontendNextN5Actions"));
        assertTrue(runtime.contains("XHFrontendNextN6"));
        assertTrue(runtime.contains("XHFrontendNextN7Auth"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("/api/admin/"));
    }

    @Test
    void realBusinessRuntimesStayIndependentFromAdminRuntime() throws Exception {
        String n2 = Files.readString(NEXT.resolve("n2.js"));
        String n3 = Files.readString(NEXT.resolve("n3.js"));
        String n4 = Files.readString(NEXT.resolve("n4.js"));
        String n5 = Files.readString(NEXT.resolve("n5.js"));
        String n6 = Files.readString(NEXT.resolve("n6-account-security.js"));
        String n7 = Files.readString(NEXT.resolve("n7-auth.js"));

        assertTrue(n2.contains("/api/services"));
        assertTrue(n3.contains("/api/quote"));
        assertTrue(n4.contains("/api/account/me"));
        assertTrue(n5.contains("/api/console/quotes"));
        assertTrue(n5.contains("/api/console/orders"));
        assertTrue(n6.contains("/api/account/password/change"));
        assertTrue(n7.contains("/api/account/register"));
        assertTrue(n7.contains("/api/account/password/reset"));
        assertFalse((n2 + n3 + n4 + n5 + n6 + n7).contains("admin-xingwei.js"));
    }
}
