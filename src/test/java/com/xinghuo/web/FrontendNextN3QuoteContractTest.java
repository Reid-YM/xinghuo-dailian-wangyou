package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FrontendNextN3QuoteContractTest {
    private static final Path MAIN = Path.of("src/main");
    private static final Path NEXT = MAIN.resolve("resources/static/next");

    @Test
    void parallelControllerLoadsN2ThenN3WithoutReplacingLegacyQuote() throws Exception {
        String controller = Files.readString(MAIN.resolve("java/com/xinghuo/web/FrontendNextPreviewController.java"));
        assertTrue(controller.contains("/next/n2.js"));
        assertTrue(controller.contains("/next/n3.js"));
        assertTrue(controller.indexOf("N2_RUNTIME") < controller.indexOf("N3_RUNTIME"));
        assertFalse(controller.contains("@GetMapping(\"/quote\")"));
    }

    @Test
    void n3UsesExistingQuoteApiWithIdempotencyAndConsent() throws Exception {
        String js = Files.readString(NEXT.resolve("n3.js"));
        assertTrue(js.contains("fetch('/api/quote'"));
        assertTrue(js.contains("method:'POST'"));
        assertTrue(js.contains("'Idempotency-Key':idempotencyKey"));
        assertTrue(js.contains("privacyConsent"));
        assertTrue(js.contains("firstTouchSource:'frontend-next'"));
        assertTrue(js.contains("button.disabled=true"));
        assertTrue(js.contains("if(submitting) return"));
        assertTrue(js.contains("Keep the same idempotency key after failures"));
    }

    @Test
    void n3DoesNotUploadSecretsOrBypassSafeDomRendering() throws Exception {
        String js = Files.readString(NEXT.resolve("n3.js"));
        assertFalse(js.contains("/api/uploads/quote"));
        assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains("localStorage"));
        assertTrue(js.contains("textContent"));
        assertTrue(js.contains("/rules"));
        assertTrue(js.contains("/privacy"));
    }
}
