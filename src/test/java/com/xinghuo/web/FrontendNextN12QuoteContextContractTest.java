package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN12QuoteContextContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void previewControllerInjectsVersionedN12Runtime() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        assertTrue(controller.contains("N12_RUNTIME"));
        assertTrue(controller.contains("/next/n12-quote-context.js"));
        assertTrue(controller.contains("next/n12-quote-context.js"));
        assertTrue(controller.contains("/next/quote"));
    }

    @Test
    void quoteContextReusesLoadedPublicCatalogWithoutAddingAnotherApi() throws Exception {
        String runtime = read("resources/static/next/n12-quote-context.js");

        assertTrue(runtime.contains("window.XHFrontendNextN2?.state?.services"));
        assertTrue(runtime.contains("window.XHFrontendNextN2?.state?.catalog"));
        assertTrue(runtime.contains("#qGame"));
        assertTrue(runtime.contains("#qCat"));
        assertTrue(runtime.contains("#qSvc"));
        assertFalse(runtime.contains("fetch("));
        assertFalse(runtime.contains("/api/quote"));
        assertFalse(runtime.contains("/api/uploads/quote"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
    }

    @Test
    void selectionAndUrlStayBidirectionallySynchronizedWithoutBreakingAttribution() throws Exception {
        String runtime = read("resources/static/next/n12-quote-context.js");

        assertTrue(runtime.contains("const url = new URL(location.href)"));
        assertTrue(runtime.contains("url.searchParams.set('service', next)"));
        assertTrue(runtime.contains("url.searchParams.delete('service')"));
        assertTrue(runtime.contains("navigation.replaceState"));
        assertFalse(runtime.contains("navigation.pushState"));
        assertTrue(runtime.contains("new URLSearchParams(location.search).get('service')"));
        assertTrue(runtime.contains("game.dispatchEvent(new Event('change'"));
        assertTrue(runtime.contains("category.dispatchEvent(new Event('change'"));
        assertTrue(runtime.contains("window.addEventListener('popstate'"));
        assertTrue(runtime.contains("window.addEventListener('xh:pagechange'"));
        assertTrue(runtime.contains("MutationObserver"));
    }

    @Test
    void quoteContextKeepsPreviewNavigationAndSafeDomRendering() throws Exception {
        String runtime = read("resources/static/next/n12-quote-context.js");
        String n3 = read("resources/static/next/n3.js");

        assertTrue(runtime.contains("/next/service/${encodeURIComponent(slug)}"));
        assertTrue(runtime.contains("/next/services"));
        assertTrue(runtime.contains("链接中的服务当前不可用"));
        assertFalse(runtime.contains("服务标识 ${slug}"));
        assertTrue(runtime.contains("重新选择"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("insertAdjacentHTML"));
        assertFalse(runtime.contains("localStorage"));
        assertFalse(runtime.contains("sessionStorage"));

        assertTrue(n3.contains("fetchWithDeadline('/api/quote'"));
        assertTrue(n3.contains("'Idempotency-Key':idempotencyKey"));
        assertTrue(n3.contains("fetchWithDeadline('/api/uploads/quote'"));
    }
}
