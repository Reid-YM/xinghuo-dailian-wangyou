package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN4AccountContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    @Test
    void parallelControllerLoadsN4AfterN3AndKeepsLegacyAccountRouteUntouched() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/web/FrontendNextPreviewController.java"));
        assertTrue(controller.contains("/next/n4.js"));
        assertTrue(controller.indexOf("N3_RUNTIME") < controller.indexOf("N4_RUNTIME"));
        assertTrue(controller.contains("\"/next/account\""));
        assertFalse(controller.contains("@GetMapping(\"/account\")"));
    }

    @Test
    void n4ReusesExistingAccountSessionAndUserScopedApis() throws Exception {
        String js = Files.readString(NEXT.resolve("n4.js"));
        assertTrue(js.contains("request('/api/account/me')"));
        assertTrue(js.contains("request('/api/account/orders?page=0&size=10')"));
        assertTrue(js.contains("request('/api/account/sessions')"));
        assertTrue(js.contains("request('/api/account/security/events')"));
        assertTrue(js.contains("request('/api/account/login'"));
        assertTrue(js.contains("request('/api/account/logout'"));
        assertTrue(js.contains("credentials: 'same-origin'"));
        assertTrue(js.contains("'X-Requested-With': 'XMLHttpRequest'"));
    }

    @Test
    void n4UsesExistingWeChatOAuthCapabilityWithoutEmbeddingSecrets() throws Exception {
        String js = Files.readString(NEXT.resolve("n4.js"));
        assertTrue(js.contains("/api/account/wechat/availability"));
        assertTrue(js.contains("/api/account/wechat/start?next="));
        assertTrue(js.contains("encodeURIComponent('/next/account')"));
        assertFalse(js.toLowerCase().contains("appsecret"));
        assertFalse(js.toLowerCase().contains("access_token"));
        assertFalse(js.toLowerCase().contains("openid"));
    }

    @Test
    void n4RendersOnlyServerReturnedAccountDataAndUsesSafeDomApis() throws Exception {
        String js = Files.readString(NEXT.resolve("n4.js"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("localStorage"));
        assertFalse(js.contains("sessionStorage"));
        assertTrue(js.contains("textContent"));
        assertTrue(js.contains("order.progressPercent"));
        assertTrue(js.contains("order.progressText"));
        assertTrue(js.contains("state.orders?.total"));
        assertTrue(js.contains("当前账号还没有已归属订单"));
    }
}
