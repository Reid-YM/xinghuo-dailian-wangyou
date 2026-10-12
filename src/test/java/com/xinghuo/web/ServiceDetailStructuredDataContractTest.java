package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServiceDetailStructuredDataContractTest {

    @Test
    void serviceDetailKeepsServiceSchemaButDoesNotAdvertiseMerchantProductSchema() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/service-detail.html"));

        assertTrue(template.contains("\"@type\":\"Service\""),
                "service detail should describe the page as a Service");
        assertTrue(template.contains("${breadcrumbJsonLd}"),
                "service detail should keep breadcrumb structured data");
        assertTrue(template.contains("${faqJsonLd}"),
                "service detail should keep FAQ structured data");
        assertFalse(template.contains("${productJsonLd}"),
                "quote-only service pages must not emit Product/Offer merchant markup");
    }
}
