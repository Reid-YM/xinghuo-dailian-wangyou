package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN23GuidesCenterContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void explicitPreviewRouteDelegatesToSharedNextDocument() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextN23GuidesController.java");
        assertTrue(controller.contains("@GetMapping(\"/next/guides\")"));
        assertTrue(controller.contains("preview.frontendNext(request)"));
    }

    @Test
    void guideCenterUsesExistingPublicReadApiOnly() throws Exception {
        String runtime = read("resources/static/next/n23-guides-center.js");
        assertTrue(runtime.contains("fetch('/api/guides?limit=100'"));
        assertTrue(runtime.contains("method: 'GET'"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));
        assertTrue(runtime.contains("request !== requestToken"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
    }

    @Test
    void listStateAndDetailNavigationStayInsideNext() throws Exception {
        String runtime = read("resources/static/next/n23-guides-center.js");
        String routes = read("resources/static/next/n14-route-continuity.js");

        assertTrue(runtime.contains("['q', 'game', 'sort']"));
        assertTrue(runtime.contains("history[mode === 'push' ? 'pushState' : 'replaceState']"));
        assertTrue(runtime.contains("/next/guides/${encodeURIComponent(slug)}"));
        assertTrue(runtime.contains("legacy.dataset.n23Legacy = '1'"));
        assertTrue(runtime.contains("canonical.href = 'https://wydlgf.cn/guides'"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));

        assertTrue(routes.contains("url.pathname === '/guides'"));
        assertTrue(routes.contains("nextUrl(url, '/next/guides')"));
        assertTrue(routes.contains("/next/n23-guides-center.js?v=20261017-n23"));
        assertTrue(routes.contains("link.dataset.n23Legacy === '1'"));
    }
}
