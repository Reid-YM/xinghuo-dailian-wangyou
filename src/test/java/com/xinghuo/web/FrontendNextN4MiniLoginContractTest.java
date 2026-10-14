package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN4MiniLoginContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    @Test
    void controllerLoadsMiniRuntimeAfterMainN4AccountRuntime() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/web/FrontendNextPreviewController.java"));
        assertTrue(controller.contains("/next/n4.js"));
        assertTrue(controller.contains("/next/n4-mini.js"));
        assertTrue(controller.indexOf("N4_RUNTIME") < controller.indexOf("N4_MINI_RUNTIME"));
        assertTrue(controller.contains("\"/next/account\""));
        assertFalse(controller.contains("@GetMapping(\"/account\")"));
    }

    @Test
    void miniRuntimeUsesExistingChallengePollAndCompleteBridge() throws Exception {
        String js = Files.readString(NEXT.resolve("n4-mini.js"));
        assertTrue(js.contains("/api/account/wechat-mini/availability"));
        assertTrue(js.contains("/api/account/wechat-mini/challenge"));
        assertTrue(js.contains("/api/account/wechat-mini/poll"));
        assertTrue(js.contains("/api/account/wechat-mini/complete"));
        assertTrue(js.contains("credentials: 'same-origin'"));
        assertTrue(js.contains("'X-Requested-With': 'fetch'"));
        assertTrue(js.contains("location.assign('/next/account')"));
        assertTrue(js.contains("data:image/png;base64,"));
    }

    @Test
    void miniRuntimeDoesNotExposeWechatSecretsOrUseUnsafeHtml() throws Exception {
        String js = Files.readString(NEXT.resolve("n4-mini.js"));
        String lower = js.toLowerCase();
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("localStorage"));
        assertFalse(js.contains("sessionStorage"));
        assertFalse(lower.contains("appsecret"));
        assertFalse(lower.contains("access_token"));
        assertFalse(lower.contains("openid"));
        assertTrue(js.contains("textContent"));
        assertTrue(js.contains("5 分钟"));
        assertTrue(js.contains("微信密码、验证码或令牌"));
    }

    @Test
    void backendBridgeRetainsShortLivedHttpOnlyCookieAndQrFallback() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/account/web/AccountWeChatMiniController.java"));
        assertTrue(controller.contains("@PostMapping(\"/challenge\")"));
        assertTrue(controller.contains("@PostMapping(\"/poll\")"));
        assertTrue(controller.contains("@PostMapping(\"/complete\")"));
        assertTrue(controller.contains("Max-Age=\" + maxAge"));
        assertTrue(controller.contains("HttpOnly; SameSite=Lax"));
        assertTrue(controller.contains("qrDataUri"));
        assertTrue(controller.contains("八位登录码仍可继续完成同一安全流程"));
    }
}
