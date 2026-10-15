package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendBackendTotalIntegrationContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void frontendNextAndV3mRuntimeResilienceCoexistWithoutCrossLoading() throws Exception {
        String site = read("resources/templates/fragments/site.html");
        String admin = read("resources/templates/admin-xingwei.html");
        String adminJs = read("resources/static/assets/admin-xingwei.js");
        assertTrue(site.contains("data-experience-shell=\"v4\""));
        assertTrue(site.contains("frontend-next-foundation.css?v=20261014nx01"));
        assertTrue(site.contains("frontend-next-services.css?v=20261014nx03"));
        assertTrue(site.contains("frontend-next-conversion.css?v=20261014nx03"));
        assertTrue(site.contains("frontend-next-account.css?v=20261014nx04"));
        assertTrue(adminJs.contains("const REQUEST_TIMEOUT_MS = 8000"));
        assertTrue(adminJs.contains("const HEALTH_CHECK_INTERVAL_MS = 60000"));
        assertTrue(adminJs.contains("window.xwRuntimeHealth"));
        assertTrue(adminJs.contains("checkBackend('visibility')"));
        assertTrue(adminJs.contains("checkBackend('online')"));
        assertTrue(adminJs.contains("dataScope: 'NONE'"));
        assertFalse(site.contains("admin-xingwei.js"), "public shell must never load admin runtime code");
        assertFalse(admin.contains("frontend-next-foundation.css"), "admin console must not inherit public visual CSS");
        assertFalse(admin.contains("frontend-next-account.css"), "account visual layer must stay out of admin console");
    }

    @Test
    void publicConversionRoutesStayPublicWhileAdminApisRemainProtected() throws Exception {
        String security = read("java/com/xinghuo/security/SecurityConfig.java");
        assertTrue(security.contains("\"/api/services\""));
        assertTrue(security.contains("\"/api/quote\""));
        assertTrue(security.contains("\"/api/records/**\""));
        assertTrue(security.contains(".requestMatchers(PERMIT_ALL).permitAll()"));
        assertTrue(security.contains(".requestMatchers(\"/admin/**\", \"/api/admin/**\")"));
        assertTrue(security.contains(".hasAnyRole(\"ADMIN\", \"OPERATOR\")"));
    }

    @Test
    void currentTotalIntegrationKeepsV3hRealPaymentOutsideTheBranch() throws Exception {
        Path migration = MAIN.resolve("resources/db/migration/V68__wechat_mini_program_payment.sql");
        Path miniPaymentIdentity = MAIN.resolve("java/com/xinghuo/payment/WeChatMiniPaymentIdentityService.java");
        assertFalse(Files.exists(migration), "V3H payment database migration must remain outside total integration until real-device validation");
        assertFalse(Files.exists(miniPaymentIdentity), "V3H mini-program payment identity service must remain outside total integration until validation");
        String controller = read("java/com/xinghuo/controller/XingweiAdminController.java");
        assertTrue(controller.contains("static final String CONSOLE_VERSION = \"20261016-v4m\""));
        assertTrue(controller.contains("\"V3H\", \"待真机验收\""));
        assertTrue(controller.contains("PR #384 保持未合并"));
        assertTrue(controller.contains("待 AppID/商户绑定和小额真机联调"));
    }

    @Test
    void accountLoginAndAccountCenterRemainOnExistingBusinessRuntime() throws Exception {
        String login = read("resources/templates/account-login.html");
        String account = read("resources/templates/account.html");
        String accountFragment = read("resources/templates/fragments/account-center.html");
        assertTrue(login.contains("wechat-mini-login.js"));
        assertTrue(login.contains("account-auth.js"));
        assertTrue(account.contains("account-center.js"));
        assertTrue(account.contains("account-center-refine.js"));
        assertTrue(account.contains("account-center-shell"));
        assertTrue(accountFragment.contains("account-center-bar"));
        assertTrue(accountFragment.contains("account-center-rail"));
        assertTrue(accountFragment.contains("account-center-nav"));
    }
}
