package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GlobalUiControlContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void sharedFrontendConsumesPersistedNavigationAndVisibilitySettings() throws IOException {
        String fragment = read("resources/templates/fragments/site.html");
        String controls = read("resources/static/assets/global-ui-controls.js");
        String settings = read("java/com/xinghuo/sitesetting/application/SiteSettingApplicationService.java");

        assertTrue(fragment.contains("/assets/global-ui-controls.js?v=20261007g005"));
        assertTrue(controls.contains("protectUserCenterFromAdminLinks"));
        for (String key : new String[]{
                "navServicesLabel", "navServicesVisible", "navGuidesLabel", "navGuidesVisible",
                "navMasteryLabel", "navMasteryVisible", "navRecordsLabel", "navRecordsVisible",
                "navContactLabel", "navContactVisible", "navAccountLabel", "navAccountVisible",
                "headerQuoteVisible", "footerServicesLabel", "footerServicesVisible",
                "footerRecordsLabel", "footerRecordsVisible", "footerConsoleLabel", "footerConsoleVisible",
                "footerRulesLabel", "footerRulesVisible", "footerPrivacyLabel", "footerPrivacyVisible",
                "footerContactLabel", "footerContactVisible", "homeQuickContactVisible",
                "icpNumber", "icpVisible", "publicSecurityNumber", "publicSecurityVisible"}) {
            assertTrue(settings.contains("\"" + key + "\""), "missing persisted setting " + key);
            assertTrue(controls.contains(key), "public frontend does not consume " + key);
        }
        assertTrue(fragment.contains("data-global-ui=\"nav-services\""));
        assertTrue(fragment.contains("data-global-visible=\"headerQuoteVisible\""));
        assertTrue(fragment.contains("data-global-ui=\"icp-number\""));
    }

    @Test
    void adminIsOrganizedByFunctionalEntryAndExposesBackendOnlyFields() throws IOException {
        String loader = read("resources/static/assets/admin-tool-station.js");
        String admin = read("resources/static/assets/admin-global-ui.js");
        String controller = read("java/com/xinghuo/sitesetting/web/GlobalUiSettingsController.java");

        assertTrue(loader.contains("/assets/admin-global-ui.js?v=20261007g004"));
        assertTrue(admin.contains("站点运营"));
        assertTrue(admin.contains("内容中心"));
        assertTrue(admin.contains("客户履约"));
        assertTrue(admin.contains("用户与系统"));
        assertTrue(admin.contains("客服与联系方式"));
        assertTrue(admin.contains("/api/admin/global-ui-settings"));
        assertTrue(controller.contains("@RequestMapping(\"/api/admin/global-ui-settings\")"));
        for (String field : new String[]{"statusShortText", "primaryButtonLabel", "toolUpdatedNote", "toolPublicEnabled"}) {
            assertTrue(admin.contains(field), "admin does not expose " + field);
        }
    }

    @Test
    void legacyBasicSaveCannotSilentlyDropExtendedStatusFields() throws IOException {
        String admin = read("resources/static/assets/admin-global-ui.js");
        assertTrue(admin.contains("#saveConfigSection"));
        assertTrue(admin.contains("saveBasicWithoutLosingExtendedFields"));
        assertTrue(admin.contains("statusShortText:$('[name=\"statusShortTextV4\"]')"));
        assertTrue(admin.contains("primaryButtonLabel:$('[name=\"primaryButtonLabelV4\"]')"));
    }

    @Test
    void anonymousFrontendCanReadIntendedPublicControlApisWhileAdminStaysProtected() throws IOException {
        String security = read("java/com/xinghuo/security/SecurityConfig.java");
        assertTrue(security.contains("\"/api/public/**\""),
                "public frontend settings/game-status APIs must remain permitAll");
        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin/**\")"),
                "admin APIs must stay protected");
    }

    @Test
    void accountPagesReachCustomSessionInterceptorInsteadOfSpringDenyAll() throws IOException {
        String security = read("java/com/xinghuo/security/SecurityConfig.java");
        for (String route : new String[]{
                "/account/requests", "/account/orders", "/account/progress",
                "/account/quotes", "/account/demands", "/account/order", "/account/order-list",
                "/account/tools", "/account/tool-progress", "/account/profile", "/account/contact",
                "/account/security", "/account/devices", "/account/activity",
                "/account/settings/profile", "/account/settings/contact", "/account/settings/security"}) {
            assertTrue(security.contains("\"" + route + "\""),
                    "account page must pass Spring Security so @RequireAccount can issue login redirect: " + route);
        }
        assertTrue(security.contains(".anyRequest().denyAll()"),
                "unlisted routes must remain denied by default");
    }

    @Test
    void toolStationPublicControlsStayInToolStationEntry() throws IOException {
        String admin = read("resources/static/assets/admin-global-ui.js");
        assertTrue(admin.contains("id=\"toolRuntimeV4\""));
        assertTrue(admin.contains("/api/admin/site-config/section/tools"));
        assertTrue(admin.contains("前台公开设置"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
