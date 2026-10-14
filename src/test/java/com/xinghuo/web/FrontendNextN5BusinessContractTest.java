package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN5BusinessContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    @Test
    void controllerLoadsN5AfterAccountAndMiniLoginRuntimes() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/web/FrontendNextPreviewController.java"));
        assertTrue(controller.contains("/next/n4.js"));
        assertTrue(controller.contains("/next/n4-mini.js"));
        assertTrue(controller.contains("/next/n5.js"));
        assertTrue(controller.indexOf("N4_RUNTIME") < controller.indexOf("N4_MINI_RUNTIME"));
        assertTrue(controller.indexOf("N4_MINI_RUNTIME") < controller.indexOf("N5_RUNTIME"));
        assertFalse(controller.contains("@GetMapping(\"/account\")"));
    }

    @Test
    void n5ConsumesExistingAccountScopedBusinessApis() throws Exception {
        String js = Files.readString(NEXT.resolve("n5.js"));
        assertTrue(js.contains("/api/console/summary"));
        assertTrue(js.contains("/api/console/quotes"));
        assertTrue(js.contains("/api/console/orders"));
        assertTrue(js.contains("/api/account/orders/candidates"));
        assertTrue(js.contains("/api/account/orders/claim"));
        assertTrue(js.contains("method: 'POST'"));
        assertTrue(js.contains("'X-Requested-With': 'XMLHttpRequest'"));
        assertTrue(js.contains("window.confirm"));
        assertTrue(js.contains("contactVerified"));
    }

    @Test
    void n5DoesNotInventBusinessDataOrLeakUnsafeHtmlState() throws Exception {
        String js = Files.readString(NEXT.resolve("n5.js"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("localStorage"));
        assertFalse(js.contains("sessionStorage"));
        assertTrue(js.contains("未返回的数据保持空白"));
        assertTrue(js.contains("候选只显示脱敏编号"));
        assertTrue(js.contains("完整订单号 / 业务编号"));
        assertTrue(js.contains("textContent"));
    }

    @Test
    void existingBackendKeepsQuotesOrdersAndClaimsAccountScoped() throws Exception {
        String console = Files.readString(MAIN.resolve("java/com/xinghuo/account/web/ConsoleApiController.java"));
        String orders = Files.readString(MAIN.resolve("java/com/xinghuo/account/web/AccountOrdersApiController.java"));
        String link = Files.readString(MAIN.resolve("java/com/xinghuo/account/service/OrderLinkService.java"));

        assertTrue(console.contains("@GetMapping(\"/quotes\")"));
        assertTrue(console.contains("@GetMapping(\"/quotes/{businessNo}\")"));
        assertTrue(console.contains("@GetMapping(\"/orders\")"));
        assertTrue(console.contains("@GetMapping(\"/orders/{businessNo}\")"));
        assertTrue(console.contains("WHERE customer_account_id = ?"));
        assertTrue(console.contains("AccountAuthInterceptor.requireAccount(request)"));

        assertTrue(orders.contains("@GetMapping(\"/candidates\")"));
        assertTrue(orders.contains("@PostMapping(\"/claim\")"));
        assertTrue(orders.contains("AccountDtos.OrderItem::ofMasked"));
        assertTrue(link.contains("!account.isContactVerified()"));
        assertTrue(link.contains("contactMatches(account"));
        assertTrue(link.contains("ORDER_ALREADY_LINKED"));
    }
}
