package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN11RelatedServicesContractTest {
    private static final Path MAIN = Path.of("src/main");

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    @Test
    void n11RuntimeIsLoadedAfterN10AndUsesContentFingerprinting() throws Exception {
        String controller = read("java/com/xinghuo/web/FrontendNextPreviewController.java");

        int n10 = controller.indexOf("if (!html.contains(N10_RUNTIME))");
        int n11 = controller.indexOf("if (!html.contains(N11_RUNTIME))");
        assertTrue(n10 >= 0 && n11 > n10);
        assertTrue(controller.contains("/next/n11-related-services.js"));
        assertTrue(controller.contains("next/n11-related-services.js"));
    }

    @Test
    void relatedServicesStayInsideTheNextPreviewAndUseOnlyPublicData() throws Exception {
        String runtime = read("resources/static/next/n11-related-services.js");

        assertTrue(runtime.contains("window.XHFrontendNextN2?.state?.services"));
        assertTrue(runtime.contains("fetch('/api/services'"));
        assertTrue(runtime.contains("code(item?.gameCode) === currentGame"));
        assertTrue(runtime.contains(".slice(0, 3)"));
        assertTrue(runtime.contains("/next/service/${encodeURIComponent(text(service?.slug))}"));
        assertTrue(runtime.contains("#n10ServiceDetail .n10-grid"));

        assertFalse(runtime.contains("/api/quote"));
        assertFalse(runtime.contains("/api/payment"));
        assertFalse(runtime.contains("/api/refund"));
        assertFalse(runtime.contains("method: 'POST'"));
        assertFalse(runtime.contains("method: 'PATCH'"));
        assertFalse(runtime.contains("method: 'PUT'"));
        assertFalse(runtime.contains("method: 'DELETE'"));
        assertFalse(runtime.contains("innerHTML"));
        assertFalse(runtime.contains("localStorage"));
    }

    @Test
    void recommendationFailureAndStaleResponsesCannotBreakThePrimaryDetail() throws Exception {
        String runtime = read("resources/static/next/n11-related-services.js");

        assertTrue(runtime.contains("let requestVersion = 0"));
        assertTrue(runtime.contains("const request = ++requestVersion"));
        assertTrue(runtime.contains("request !== requestVersion || routeSlug() !== slug"));
        assertTrue(runtime.contains("catch (_)"));
        assertTrue(runtime.contains("removeRelated()"));
        assertTrue(runtime.contains("queueMicrotask"));
        assertTrue(runtime.contains("new MutationObserver(schedule)"));
        assertTrue(runtime.contains("event.target instanceof Element"));
    }
}
