package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GlobalUiControlContractTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void publicPagesUseSingleFloatingCustomerServiceEntry() throws IOException {
        String fragment = read("resources/templates/fragments/site.html");
        String controls = read("resources/static/assets/global-ui-controls.js");

        assertTrue(fragment.contains("class=\"native-customer-service\""));
        assertTrue(fragment.contains("/assets/global-ui-controls.js"));
        assertTrue(controls.contains(".quick-contact-card,[data-embedded-customer-service]"));
        assertTrue(controls.contains("a[href=\"/contact\"],a[href^=\"/support/\"]"));
        assertTrue(controls.contains("link.closest('.native-customer-service')"));
    }

    @Test
    void sharedFrontendConsumesPersistedGlobalSettings() throws IOException {
        String fragment = read("resources/templates/fragments/site.html");
        String controls = read("resources/static/assets/global-ui-controls.js");
        String settings = read("java/com/xinghuo/service/SiteSettingService.java");

        for (String key : new String[]{
                "floatingCustomerServiceEnabled", "navServicesLabel", "navGuidesLabel",
                "navMasteryLabel", "navRecordsLabel", "navAccountLabel", "footerServicesLabel",
                "footerRecordsLabel", "footerAccountLabel", "footerRulesLabel", "footerPrivacyLabel",
                "floatingCustomerServiceLabel", "floatingCustomerServiceTitle",
                "floatingCustomerServiceIntro", "floatingCustomerServiceNote",
                "icpNumber", "publicSecurityNumber"}) {
            assertTrue(settings.contains("\"" + key + "\""), "missing persisted setting " + key);
            assertTrue(controls.contains(key), "public frontend does not consume " + key);
        }
        assertTrue(fragment.contains("data-global-ui=\"customer-service-label\""));
        assertTrue(fragment.contains("data-global-ui=\"icp-number\""));
    }

    @Test
    void adminExposesPreviouslyBackendOnlyControls() throws IOException {
        String adminLoader = read("resources/static/assets/admin-tool-station.js");
        String admin = read("resources/static/assets/admin-global-ui.js");
        String controller = read("java/com/xinghuo/controller/GlobalUiSettingsController.java");

        assertTrue(adminLoader.contains("/assets/admin-global-ui.js"));
        assertTrue(admin.contains("/api/admin/global-ui-settings"));
        assertTrue(controller.contains("@RequestMapping(\"/api/admin/global-ui-settings\")"));
        for (String field : new String[]{
                "statusShortText", "primaryButtonLabel", "toolUpdatedNote", "toolPublicEnabled"}) {
            assertTrue(admin.contains(field), "admin does not expose " + field);
        }
    }

    @Test
    void anonymousFrontendCanReadIntendedPublicControlApis() throws IOException {
        String security = read("java/com/xinghuo/security/SecurityConfig.java");
        assertTrue(security.contains("\"/api/public/**\""),
                "public frontend settings/game-status APIs must remain permitAll");
        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin/**\")"),
                "admin APIs must stay protected");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
