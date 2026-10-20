package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN6AccountSecurityContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    @Test
    void controllerLoadsN6AfterN5ActionsWithoutReplacingLegacyAccount() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/web/FrontendNextPreviewController.java"));
        assertTrue(controller.contains("/next/n5-actions.js"));
        assertTrue(controller.contains("/next/n6-account-security.js"));
        assertTrue(controller.indexOf("N5_ACTIONS_RUNTIME") < controller.indexOf("N6_RUNTIME"));
        assertFalse(controller.contains("@GetMapping(\"/account\")"));
    }

    @Test
    void n6UsesExistingProfileEmailPasswordAndSessionApis() throws Exception {
        String js = Files.readString(NEXT.resolve("n6-account-security.js"));
        assertTrue(js.contains("/api/account/profile"));
        assertTrue(js.contains("/api/account/email/verify/request"));
        assertTrue(js.contains("/api/account/email/verify/confirm"));
        assertTrue(js.contains("/api/account/password/change"));
        assertTrue(js.contains("/api/account/sessions/"));
        assertTrue(js.contains("/api/account/sessions/revoke-all"));
        assertTrue(js.contains("'X-Requested-With': 'fetch'"));
        assertTrue(js.contains("credentials: 'same-origin'"));
        assertTrue(js.contains("window.confirm"));
    }

    @Test
    void n6KeepsSensitiveStateOutOfBrowserStorageAndDoesNotExposeSessionIds() throws Exception {
        String js = Files.readString(NEXT.resolve("n6-account-security.js"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("localStorage"));
        assertFalse(js.contains("sessionStorage"));
        assertTrue(js.contains("textContent"));
        assertTrue(js.contains("可退出不再使用的设备"));
        assertTrue(js.contains("不要把验证码发送给其他人"));
    }

    @Test
    void backendRetainsPasswordAndSessionSecurityRules() throws Exception {
        String account = Files.readString(MAIN.resolve("java/com/xinghuo/account/web/AccountApiController.java"));
        String email = Files.readString(MAIN.resolve("java/com/xinghuo/account/web/AccountEmailApiController.java"));
        String service = Files.readString(MAIN.resolve("java/com/xinghuo/account/service/AccountService.java"));
        String policy = Files.readString(MAIN.resolve("java/com/xinghuo/account/service/PasswordPolicy.java"));

        assertTrue(account.contains("@PostMapping(\"/profile\")"));
        assertTrue(account.contains("@PostMapping(\"/password/change\")"));
        assertTrue(account.contains("@DeleteMapping(\"/sessions/{id}\")"));
        assertTrue(account.contains("@PostMapping(\"/sessions/revoke-all\")"));
        assertTrue(account.contains("只能下线自己的会话"));
        assertTrue(email.contains("@PostMapping(\"/email/verify/request\")"));
        assertTrue(email.contains("@PostMapping(\"/email/verify/confirm\")"));
        assertTrue(service.contains("sessionService.revokeAll(account.getId())"));
        assertTrue(service.contains("sessionService.clearCookie(response)"));
        assertTrue(policy.contains("MIN_LENGTH = 8"));
        assertTrue(policy.contains("kinds < 2"));
        assertTrue(policy.contains("MAX_BYTES = 72"));
    }
}
