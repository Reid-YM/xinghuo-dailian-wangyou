package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApiApplicationBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void featureApplicationPackagesOwnTheirContracts() throws IOException {
        for (String feature : new String[] {"quote", "order", "catalog"}) {
            Path application = ROOT.resolve("src/main/java/com/xinghuo/" + feature + "/application");
            try (Stream<Path> files = Files.list(application)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    assertFalse(source.contains("com.xinghuo.dto"), file + " must not depend on web/legacy DTOs");
                    assertFalse(source.contains("com.xinghuo.model"), file + " must not depend on persistence entities");
                    assertFalse(source.contains("com.xinghuo.repository"), file + " must not depend on repositories");
                    assertFalse(source.contains("jakarta.servlet"), file + " must not depend on servlet APIs");
                    assertFalse(source.contains("jakarta.persistence"), file + " must not depend on JPA");
                }
            }
        }
    }

    @Test
    void webLayerOwnsProtocolMapping() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        String quoteMapper = read("src/main/java/com/xinghuo/quote/web/QuoteWebMapper.java");
        String orderMapper = read("src/main/java/com/xinghuo/order/web/OrderWebMapper.java");
        String catalogMapper = read("src/main/java/com/xinghuo/catalog/web/CatalogWebMapper.java");

        assertTrue(controller.contains("QuoteWebMapper.toCommand"));
        assertTrue(controller.contains("QuoteWebMapper.toResponse"));
        assertTrue(controller.contains("OrderWebMapper.toResponse"));
        assertTrue(controller.contains("OrderWebMapper.toCaseResponses"));
        assertTrue(controller.contains("CatalogWebMapper.toResponses"));

        assertTrue(quoteMapper.contains("PublicQuoteRequest"));
        assertTrue(quoteMapper.contains("ApiDtos.QuoteResponse"));
        assertTrue(orderMapper.contains("ApiDtos.OrderPublicResponse"));
        assertTrue(catalogMapper.contains("ApiDtos.ServiceResponse"));
    }

    @Test
    void infrastructureContainsLegacyTranslation() throws IOException {
        String quote = read("src/main/java/com/xinghuo/quote/infrastructure/LegacyQuoteAdapter.java");
        String order = read("src/main/java/com/xinghuo/order/infrastructure/LegacyOrderAdapter.java");
        String catalog = read("src/main/java/com/xinghuo/catalog/infrastructure/LegacyCatalogAdapter.java");

        assertTrue(quote.contains("PublicQuoteRequest"));
        assertTrue(quote.contains("ApiDtos.QuoteResponse"));
        assertTrue(order.contains("ApiDtos.OrderPublicResponse"));
        assertTrue(catalog.contains("ApiDtos.ServiceResponse"));
    }

    @Test
    void applicationContractsAreExplicitTypedRecords() throws IOException {
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteSubmissionCommand.java")
                .contains("record QuoteSubmissionCommand"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteSubmissionResult.java")
                .contains("record QuoteSubmissionResult"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/OrderPublicView.java")
                .contains("record OrderPublicView"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/PublicCaseView.java")
                .contains("record PublicCaseView"));
        assertTrue(read("src/main/java/com/xinghuo/catalog/application/CatalogServiceView.java")
                .contains("record CatalogServiceView"));
    }
}
