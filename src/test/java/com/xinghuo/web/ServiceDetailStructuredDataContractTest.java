package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServiceDetailStructuredDataContractTest {

    @Test
    void serviceDetailKeepsServiceAndDeduplicatedBreadcrumbSchemasWithoutMerchantProduct() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates/service-detail.html"));

        assertTrue(template.contains("\"@type\":\"Service\""),
                "service detail should describe the page as a Service");
        assertTrue(template.contains("\"@type\":\"BreadcrumbList\""),
                "service detail should keep breadcrumb structured data");
        assertTrue(template.contains("\"position\":3,\"name\":[[${serviceDisplayH1}]],\"item\":[[${canonicalUrl}]]"),
                "breadcrumb leaf must use the same deduplicated public service title and canonical URL");
        assertTrue(template.contains("${faqJsonLd}"),
                "service detail should keep FAQ structured data");
        assertFalse(template.contains("${breadcrumbJsonLd}"),
                "legacy controller breadcrumb can reintroduce duplicated game tokens and must not drive this template");
        assertFalse(template.contains("${productJsonLd}"),
                "quote-only service pages must not emit Product/Offer merchant markup");
    }
}
