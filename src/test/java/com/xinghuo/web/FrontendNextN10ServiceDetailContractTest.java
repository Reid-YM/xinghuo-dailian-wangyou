package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN10ServiceDetailContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void nextPreviewExposesDedicatedServiceDetailRouteWithoutReplacingLegacyDetail() throws Exception {
        String previewController = read("java/com/xinghuo/web/FrontendNextPreviewController.java");
        String pageController = read("java/com/xinghuo/controller/PageController.java");

        assertTrue(previewController.contains("/next/service/{slug}"));
        assertTrue(previewController.contains("/next/n10-service-detail.js"));
        assertTrue(previewController.contains("next/n10-service-detail.js"));
        assertTrue(previewController.contains("noindex,follow"));

        assertTrue(pageController.contains("/service/{slug}"));
        assertTrue(pageController.contains("return \"service-detail\""));
        assertFalse(pageController.contains("redirect:/next/service"));
    }

    @Test
    void nextDetailReadsOnlyExistingPublicCatalogAndServiceData() throws Exception {
        String runtime = read("resources/static/next/n10-service-detail.js");

        assertTrue(runtime.contains("getJson('/api/services')"));
        assertTrue(runtime.contains("getJson('/api/catalog')"));
        assertTrue(runtime.contains("window.XHFrontendNextN2?.state"));
        assertTrue(runtime.contains("credentials: 'same-origin'"));
        assertTrue(runtime.contains("cache: 'no-store'"));

        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("/api/quote"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
        assertFalse(runtime.contains("/api/console/"));
    }

    @Test
    void nextInternalLinksStayInPreviewWhileExplicitLegacyEscapeRemainsAvailable() throws Exception {
        String runtime = read("resources/static/next/n10-service-detail.js");
        String n2 = read("resources/static/next/n2.js");

        assertTrue(runtime.contains("link.setAttribute('href', runtimeHref('/next' + raw))"));
        assertTrue(runtime.contains("MutationObserver"));
        assertTrue(runtime.contains("link.dataset.n10Legacy === '1'"));
        assertTrue(runtime.contains("legacy.dataset.n10Legacy = '1'"));
        assertTrue(runtime.contains("/next/quote?service=${encodeURIComponent(slug)}"));
        assertTrue(runtime.contains("/next/services"));
        assertTrue(runtime.contains("canonical.href = `https://wydlgf.cn/service/${encodeURIComponent(text(service?.slug))}`"));
        assertTrue(runtime.contains("robots.content = 'noindex,follow'"));

        assertTrue(n2.contains("new URLSearchParams(location.search).get('service')"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("localStorage"));
    }

    @Test
    void slowOrObsoleteDetailResponsesCannotOverwriteTheCurrentRoute() throws Exception {
        String runtime = read("resources/static/next/n10-service-detail.js");

        assertTrue(runtime.contains("let routeRequest = 0"));
        assertTrue(runtime.contains("const request = ++routeRequest"));
        assertTrue(runtime.contains("request !== routeRequest || detailRoute() !== slug"));
        assertTrue(runtime.contains("renderLoading(section, slug)"));
        assertTrue(runtime.contains("data-n10-retry") || runtime.contains("dataset.n10Retry"));
        assertTrue(runtime.contains("if (retry) syncRoute()"));
        assertTrue(runtime.contains("event.target instanceof Element"));
        assertFalse(runtime.contains("setTimeout("));
    }
}
