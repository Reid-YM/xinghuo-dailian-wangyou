package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN2ContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    @Test
    void parallelControllerInjectsN2RuntimeWithoutReplacingLegacyRoutes() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/web/FrontendNextPreviewController.java"));
        assertTrue(controller.contains("/next/n2.js"));
        assertTrue(controller.contains("/next/services"));
        assertTrue(controller.contains("/next/quote"));
        assertTrue(controller.contains("/next/account"));
        assertTrue(controller.contains("CacheControl.noStore()"));
        assertFalse(controller.contains("@GetMapping(\"/\")"));
    }

    @Test
    void n2OnlyReadsExistingPublicCatalogAndServices() throws Exception {
        String js = Files.readString(NEXT.resolve("n2.js"));
        assertTrue(js.contains("getJson('/api/catalog',controller.signal)"));
        assertTrue(js.contains("getJson('/api/services',controller.signal)"));
        assertTrue(js.contains("method:'GET'"));
        assertTrue(js.contains("Promise.allSettled"));
        assertTrue(js.contains("const serverRenderedHall=$('#n23-services-ssr');"));
        assertTrue(js.contains("serverRenderedHall.remove();"));
        assertTrue(js.contains("style.removeProperty('display')"));
        assertFalse(js.contains("/api/quote"));
        assertFalse(js.contains("method:'POST'"));
        assertFalse(js.contains("innerHTML"));
    }

    @Test
    void n2NeverUsesDemoProductsAsProductionFallback() throws Exception {
        String html = Files.readString(NEXT.resolve("index.html"));
        String js = Files.readString(NEXT.resolve("n2.js"));
        assertTrue(html.contains("serviceGrid"));
        assertFalse(html.contains("演示模式：不会向生产环境发送请求"));
        assertTrue(js.contains("未使用演示服务或价格"));
        assertTrue(js.contains("重新读取"));
    }
}
