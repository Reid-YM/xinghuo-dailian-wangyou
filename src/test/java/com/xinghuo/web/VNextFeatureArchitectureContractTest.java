package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VNextFeatureArchitectureContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void pagesComposeFeaturesInsteadOfOwningTransportAndBusinessRules() throws IOException {
        String quote = read("frontend-vnext/src/pages/QuotePage.tsx");
        String records = read("frontend-vnext/src/pages/RecordsPage.tsx");
        String services = read("frontend-vnext/src/pages/ServicesPage.tsx");

        assertTrue(quote.contains("useQuoteFlow"));
        assertFalse(quote.contains("submitQuote"));
        assertFalse(quote.contains("uploadQuoteScreenshot"));
        assertFalse(quote.contains("loadAccountMe"));
        assertFalse(quote.contains("../lib/api"));

        assertTrue(records.contains("useBusinessRecord"));
        assertFalse(records.contains("lookupBusinessNo"));
        assertFalse(records.contains("pickText"));
        assertFalse(records.contains("pickNumber"));
        assertFalse(records.contains("../lib/api"));

        assertTrue(services.contains("filterServices"));
        assertTrue(services.contains("features/services/ServiceCard"));
        assertFalse(services.contains("../lib/api"));
    }

    @Test
    void featureModulesOwnQuoteRecordAndServiceBehavior() throws IOException {
        String quoteFlow = read("frontend-vnext/src/features/quote/useQuoteFlow.ts");
        String recordFlow = read("frontend-vnext/src/features/records/useBusinessRecord.ts");
        String recordModel = read("frontend-vnext/src/features/records/model.ts");
        String serviceModel = read("frontend-vnext/src/features/services/model.ts");
        String sharedPresentation = read("frontend-vnext/src/components/public-content.tsx");

        assertTrue(quoteFlow.contains("submitQuote"));
        assertTrue(quoteFlow.contains("uploadQuoteScreenshot"));
        assertTrue(quoteFlow.contains("loadAccountMe"));
        assertTrue(quoteFlow.contains("privacyConsent: true"));
        assertTrue(recordFlow.contains("lookupBusinessNo"));
        assertTrue(recordModel.contains("toBusinessRecordView"));
        assertTrue(serviceModel.contains("filterServices"));
        assertTrue(serviceModel.contains("servicePrice"));
        assertFalse(sharedPresentation.contains("servicePrice"));
        assertFalse(sharedPresentation.contains("JsonRecord"));
    }
}
