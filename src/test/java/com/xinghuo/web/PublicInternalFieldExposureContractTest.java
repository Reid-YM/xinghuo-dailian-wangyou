package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PublicInternalFieldExposureContractTest {
    private static final Path MAIN = Path.of("src/main");

    @Test
    void publicApiSanitizerCoversFrontendPayloadsAndInternalKeys() throws Exception {
        String advice = Files.readString(MAIN.resolve("java/com/xinghuo/controller/PublicResponseSanitizerAdvice.java"));

        assertTrue(advice.contains("\"/api/site-config\""));
        assertTrue(advice.contains("\"/api/services\""));
        assertTrue(advice.contains("\"/api/catalog\""));
        assertTrue(advice.contains("\"/api/quote\""));
        assertTrue(advice.contains("\"id\""));
        assertTrue(advice.contains("\"internalNote\""));
        assertTrue(advice.contains("\"version\""));
        assertTrue(advice.contains("\"sourceQuoteId\""));
        assertTrue(advice.contains("\"customerIpHash\""));
        assertTrue(advice.contains("INTERNAL_FIELDS.forEach(object::remove)"));
    }

    @Test
    void publicShellRemovesServiceDatabaseIdFromRenderedCards() throws Exception {
        String site = Files.readString(MAIN.resolve("resources/templates/fragments/site.html"));
        String sanitizer = Files.readString(MAIN.resolve("resources/static/assets/public-field-sanitizer.js"));

        assertTrue(site.contains("/assets/public-field-sanitizer.js?v=20261007pfs01"));
        assertTrue(sanitizer.contains("removeAttribute('data-service-id')"));
        assertTrue(sanitizer.contains("setAttribute('data-ref', slug)"));
        assertFalse(sanitizer.contains("service.id"));
    }
}
