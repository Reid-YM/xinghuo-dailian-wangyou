package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN5ActionsContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    @Test
    void controllerLoadsActionsAfterN5BusinessRuntime() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/web/FrontendNextPreviewController.java"));
        assertTrue(controller.contains("/next/n5.js"));
        assertTrue(controller.contains("/next/n5-actions.js"));
        assertTrue(controller.indexOf("N5_RUNTIME") < controller.indexOf("N5_ACTIONS_RUNTIME"));
        assertFalse(controller.contains("@GetMapping(\"/account\")"));
    }

    @Test
    void actionsUseExistingAccountScopedWriteEndpointsAndLimits() throws Exception {
        String js = Files.readString(NEXT.resolve("n5-actions.js"));
        assertTrue(js.contains("/supplement"));
        assertTrue(js.contains("/after-sales"));
        assertTrue(js.contains("/followups"));
        assertTrue(js.contains("max: 1200"));
        assertTrue(js.contains("content.maxLength = 240"));
        assertTrue(js.contains("input.maxLength = 240"));
        assertTrue(js.contains("'DELIVERY'"));
        assertTrue(js.contains("'CANCELLATION'"));
        assertTrue(js.contains("'REFUND_REVIEW'"));
        assertTrue(js.contains("'OTHER'"));
        assertTrue(js.contains("'X-Requested-With': 'fetch'"));
        assertTrue(js.contains("credentials: 'same-origin'"));
    }

    @Test
    void actionsAvoidUnsafeClientPersistenceAndSensitiveCredentialPrompts() throws Exception {
        String js = Files.readString(NEXT.resolve("n5-actions.js"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("localStorage"));
        assertFalse(js.contains("sessionStorage"));
        assertTrue(js.contains("textContent"));
        assertTrue(js.contains("请勿填写密码、短信验证码或其他登录凭据"));
    }

    @Test
    void backendRetainsAccountOwnershipValidationAndServerSideLimits() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/account/web/ConsoleApiController.java"));
        assertTrue(controller.contains("@PostMapping(\"/quotes/{businessNo}/supplement\")"));
        assertTrue(controller.contains("@PostMapping(\"/orders/{businessNo}/supplement\")"));
        assertTrue(controller.contains("@PostMapping(\"/orders/{businessNo}/after-sales\")"));
        assertTrue(controller.contains("@PostMapping(\"/orders/{businessNo}/after-sales/records/{recordId}/followups\")"));
        assertTrue(controller.contains("@Size(max = 1200)"));
        assertTrue(controller.contains("@Size(max = 240)"));
        assertTrue(controller.contains("AccountAuthInterceptor.requireAccount(request)"));
        assertTrue(controller.contains("WHERE customer_account_id = ?"));
        assertTrue(controller.contains("售后记录不存在或不属于当前订单"));
    }
}
