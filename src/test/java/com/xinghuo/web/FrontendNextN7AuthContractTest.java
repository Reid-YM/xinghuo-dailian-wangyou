package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN7AuthContractTest {
    private static final Path NEXT = Path.of("src/main/resources/static/next");
    private static final Path CONTROLLER = Path.of("src/main/java/com/xinghuo/web/FrontendNextPreviewController.java");

    @Test
    void loadsN7AfterN6AndReusesExistingAccountEndpoints() throws Exception {
        String controller = Files.readString(CONTROLLER);
        String runtime = Files.readString(NEXT.resolve("n7-auth.js"));

        assertTrue(controller.contains("N7_RUNTIME"));
        assertTrue(controller.indexOf("N6_RUNTIME") < controller.indexOf("N7_RUNTIME"));
        assertTrue(controller.contains("/next/n7-auth.js"));

        assertTrue(runtime.contains("option.value = 'EMAIL'"));
        assertTrue(runtime.contains("/api/account/register/verify/request"));
        assertTrue(runtime.contains("/api/account/register"));
        assertTrue(runtime.contains("/api/account/password/reset/request"));
        assertTrue(runtime.contains("/api/account/password/reset"));
        assertTrue(runtime.contains("contactType: 'EMAIL'"));
        assertTrue(runtime.contains("'X-Requested-With': 'XMLHttpRequest'"));
        assertTrue(runtime.contains("window.XHFrontendNextN4Mini?.stop?.()"));
        assertTrue(runtime.contains("旧版注册页"));
        assertTrue(runtime.contains("旧版找回页"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
    }

    @Test
    void backendRemainsEmailOnlyAndN7DoesNotCreateASecondAuthModel() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/xinghuo/account/service/AccountService.java"));
        String emailController = Files.readString(Path.of("src/main/java/com/xinghuo/account/web/AccountEmailApiController.java"));
        String runtime = Files.readString(NEXT.resolve("n7-auth.js"));

        assertTrue(service.contains("当前账号体系只允许邮箱注册与邮箱登录"));
        assertTrue(service.contains("EMAIL_ONLY"));
        assertTrue(emailController.contains("/register/verify/request"));
        assertTrue(emailController.contains("/password/reset/request"));
        assertTrue(emailController.contains("/password/reset"));
        assertTrue(runtime.contains("当前账号密码体系仅支持邮箱登录"));
        assertFalse(runtime.contains("/api/next/account"));
    }
}
