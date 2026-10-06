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
    void adminExposesPreviouslyBackendOnlyControlsInMatchingEntrypoints() throws IOException {
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
        for (String entry : new String[]{
                "站点运营", "全局导航与备案", "站点状态", "首页内容", "首页视觉",
                "客服中心", "内容中心", "游戏运营状态", "客户履约", "用户与系统"}) {
            assertTrue(admin.contains(entry), "admin navigation missing " + entry);
        }
    }

    @Test
    void customerServiceRoutingCanBeChangedFromAdminWithoutRestart() throws IOException {
        String admin = read("resources/static/assets/admin-global-ui.js");
        String settings = read("java/com/xinghuo/service/SiteSettingService.java");
        String controller = read("java/com/xinghuo/controller/CustomerServiceController.java");
        String adminController = read("java/com/xinghuo/controller/CustomerServiceSettingsController.java");

        assertTrue(admin.contains("/api/admin/customer-service-settings"));
        assertTrue(adminController.contains("@RequestMapping(\"/api/admin/customer-service-settings\")"));
        for (String key : new String[]{
                "customerServiceEnabled", "customerServiceDefaultUrl", "customerServiceGameUrl",
                "customerServiceOrderUrl", "customerServiceAfterSalesUrl", "customerServiceBusinessUrl"}) {
            assertTrue(settings.contains("\"" + key + "\""), "settings persistence missing " + key);
            assertTrue(admin.contains(key), "admin customer center missing " + key);
        }
        assertTrue(controller.contains("siteSettings.get(\"customerServiceEnabled\""));
        assertTrue(controller.contains("siteSettings.get(topicKey"));
        assertTrue(settings.contains("work.weixin.qq.com"));
    }

    @Test
    void supportPagesConsumeCustomerHoursAndSafetyCopy() throws IOException {
        String controls = read("resources/static/assets/global-ui-controls.js");
        String contact = read("resources/templates/contact.html");
        String support = read("resources/templates/customer-service.html");

        assertTrue(controls.contains("data-global-ui=\"service-hours\""));
        assertTrue(controls.contains("data-global-ui=\"customer-service-safety\""));
        assertTrue(contact.contains("data-global-ui=\"service-hours\""));
        assertTrue(contact.contains("data-global-ui=\"customer-service-safety\""));
        assertTrue(support.contains("data-global-ui=\"customer-service-safety\""));
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
