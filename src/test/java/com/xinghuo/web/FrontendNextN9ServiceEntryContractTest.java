package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN9ServiceEntryContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void rolloutDefaultsToLegacyAndOnlySupportsKnownModes() throws Exception {
        String service = read("java/com/xinghuo/service/SiteSettingService.java");
        String publicRuntime = read("resources/static/assets/global-ui-controls.js");

        assertTrue(service.contains("Map.entry(\"servicesEntryMode\", \"legacy\")"));
        assertTrue(service.contains("Set.of(\"legacy\", \"next\")"));
        assertTrue(service.contains("服务入口目标仅支持 legacy 或 next"));
        assertTrue(publicRuntime.contains("? '/next/services'"));
        assertTrue(publicRuntime.contains(": '/services'"));
        assertTrue(publicRuntime.contains("[data-global-ui=\"nav-services\"]"));
        assertTrue(publicRuntime.contains("[data-global-ui=\"footer-services\"]"));
        assertTrue(publicRuntime.contains(".nav-drawer a[href=\"/services\"], .nav-drawer a[href=\"/next/services\"]"));
        assertTrue(publicRuntime.contains("xh:site-config"));
        assertFalse(publicRuntime.contains("servicesEntryUrl"));
    }

    @Test
    void rolloutAdminPageUsesExistingAuditedGlobalUiEndpoint() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextRolloutAdminController.java");
        String template = read("resources/templates/admin-xingwei-frontend-next.html");
        String runtime = read("resources/static/assets/admin-frontend-next-rollout.js");
        String globalController = read("java/com/xinghuo/controller/GlobalUiSettingsController.java");

        assertTrue(controller.contains("/admin/xingwei/frontend-next"));
        assertTrue(template.contains("servicesEntryMode"));
        assertTrue(template.contains("旧版稳定入口 · /services"));
        assertTrue(template.contains("新版候选入口 · /next/services"));
        assertTrue(template.contains("/admin/xingwei/frontend-controls"));
        assertTrue(runtime.contains("/api/admin/global-ui-settings"));
        assertTrue(runtime.contains("servicesEntryMode: requested"));
        assertTrue(runtime.contains("Frontend Next N9"));
        assertTrue(globalController.contains("GLOBAL_UI_SETTINGS_UPDATE"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
    }

    @Test
    void rolloutDoesNotRedirectLegacyRoutesOrTouchBusinessWrites() throws Exception {
        String pageController = read("java/com/xinghuo/controller/PageController.java");
        String rolloutController = read("java/com/xinghuo/web/FrontendNextRolloutAdminController.java");
        String runtime = read("resources/static/assets/admin-frontend-next-rollout.js");

        assertTrue(pageController.contains("@GetMapping(\"/services\")"));
        assertFalse(pageController.contains("redirect:/next/services"));
        assertFalse(rolloutController.contains("@PostMapping"));
        assertFalse(runtime.contains("/api/quote"));
        assertFalse(runtime.contains("/api/console/"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }
}
