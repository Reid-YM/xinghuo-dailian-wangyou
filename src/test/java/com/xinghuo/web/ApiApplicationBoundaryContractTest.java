package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApiApplicationBoundaryContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void featureApplicationPackagesOwnPersistenceIndependentContracts() throws IOException {
        for (String feature : List.of(
                "quote", "order", "catalog", "record", "dashboard", "feedback", "taxonomy", "slot", "siteconfig")) {
            Path application = ROOT.resolve("src/main/java/com/xinghuo/" + feature + "/application");
            try (Stream<Path> files = Files.walk(application)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    assertFalse(source.contains("com.xinghuo.dto"), file + " must not depend on protocol DTOs");
                    assertFalse(source.contains("com.xinghuo.model"), file + " must not depend on global persistence entities");
                    assertFalse(source.contains("com.xinghuo.repository"), file + " must not depend on retired repositories");
                    assertFalse(source.contains("jakarta.servlet"), file + " must not depend on servlet APIs");
                    assertFalse(source.contains("jakarta.persistence"), file + " must not depend on JPA");
                    assertFalse(source.contains("org.springframework.data."), file + " must not depend on Spring Data");
                }
            }
        }
    }

    @Test
    void webLayerOwnsProtocolMappingAndApplicationInvocation() throws IOException {
        String publicQuote = read("src/main/java/com/xinghuo/quote/web/PublicQuoteController.java");
        String publicOrder = read("src/main/java/com/xinghuo/order/web/PublicOrderController.java");
        String publicRecord = read("src/main/java/com/xinghuo/record/web/PublicRecordController.java");
        String publicServices = read("src/main/java/com/xinghuo/catalog/web/PublicCatalogServiceController.java");
        String publicCatalog = read("src/main/java/com/xinghuo/taxonomy/web/PublicTaxonomyCatalogController.java");
        String adminDashboard = read("src/main/java/com/xinghuo/dashboard/web/AdminDashboardController.java");

        assertTrue(publicQuote.contains("QuoteApplicationService"));
        assertTrue(publicQuote.contains("QuoteWebMapper.toCommand"));
        assertTrue(publicQuote.contains("QuoteWebMapper.toResponse"));
        assertFalse(publicQuote.contains("QuoteRepository"));

        assertTrue(publicOrder.contains("OrderApplicationService"));
        assertTrue(publicOrder.contains("OrderWebMapper.toResponse"));
        assertFalse(publicOrder.contains("CustomerOrderRepository"));

        assertTrue(publicRecord.contains("RecordApplicationService"));
        assertTrue(publicRecord.contains("RecordWebMapper.toResponse"));
        assertFalse(publicRecord.contains("QuoteRepository"));
        assertFalse(publicRecord.contains("CustomerOrderRepository"));

        assertTrue(publicServices.contains("CatalogApplicationService"));
        assertTrue(publicServices.contains("CatalogWebMapper.toResponses"));
        assertFalse(publicServices.contains("ServiceItemRepository"));

        assertTrue(publicCatalog.contains("TaxonomyApplicationService"));
        assertTrue(publicCatalog.contains("TaxonomyWebMapper.toResponse"));
        assertFalse(publicCatalog.contains("GameCatalogRepository"));

        assertTrue(adminDashboard.contains("DashboardApplicationService"));
        assertTrue(adminDashboard.contains("DashboardWebMapper.toResponse"));
        assertFalse(adminDashboard.contains("Repository"));
    }

    @Test
    void webMappersOwnProtocolTranslationWithoutGlobalDtos() throws IOException {
        String quoteMapper = read("src/main/java/com/xinghuo/quote/web/QuoteWebMapper.java");
        String orderMapper = read("src/main/java/com/xinghuo/order/web/OrderWebMapper.java");
        String recordMapper = read("src/main/java/com/xinghuo/record/web/RecordWebMapper.java");
        String dashboardMapper = read("src/main/java/com/xinghuo/dashboard/web/DashboardWebMapper.java");
        String taxonomyMapper = read("src/main/java/com/xinghuo/taxonomy/web/TaxonomyWebMapper.java");
        String catalogMapper = read("src/main/java/com/xinghuo/catalog/web/CatalogWebMapper.java");

        assertFalse(quoteMapper.contains("com.xinghuo.dto"));
        assertTrue(quoteMapper.contains("QuoteResponse"));
        assertTrue(quoteMapper.contains("QuoteAdminResponse"));
        assertTrue(quoteMapper.contains("QuoteConvertedOrderResponse"));
        assertTrue(quoteMapper.contains("QuoteSubmissionCommand"));
        assertFalse(orderMapper.contains("com.xinghuo.dto"));
        assertTrue(orderMapper.contains("AdminOrderResponse"));
        assertTrue(orderMapper.contains("OrderPublicResponse"));
        assertTrue(orderMapper.contains("OrderPublicView"));
        assertFalse(recordMapper.contains("com.xinghuo.dto"));
        assertTrue(recordMapper.contains("RecordPublicResponse"));
        assertTrue(recordMapper.contains("RecordPublicView"));
        assertFalse(dashboardMapper.contains("com.xinghuo.dto"));
        assertTrue(dashboardMapper.contains("DashboardResponse"));
        assertTrue(dashboardMapper.contains("DashboardView"));
        assertFalse(taxonomyMapper.contains("com.xinghuo.dto"));
        assertTrue(taxonomyMapper.contains("TaxonomyCatalogView"));
        assertTrue(taxonomyMapper.contains("CatalogResponse"));
        assertFalse(catalogMapper.contains("com.xinghuo.dto"));
        assertTrue(catalogMapper.contains("CatalogServiceView"));
        assertTrue(catalogMapper.contains("ServiceResponse"));
    }

    @Test
    void infrastructureOwnsPersistenceTranslationAndCrossFeatureMetricsReads() throws IOException {
        String quote = read("src/main/java/com/xinghuo/quote/infrastructure/QuotePersistenceAdapter.java");
        String order = read("src/main/java/com/xinghuo/order/infrastructure/OrderPersistenceAdapter.java");
        String catalog = read("src/main/java/com/xinghuo/catalog/infrastructure/CatalogPersistenceAdapter.java");
        String taxonomy = read("src/main/java/com/xinghuo/taxonomy/infrastructure/TaxonomyPersistenceAdapter.java");
        String dashboard = read("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java");

        assertTrue(quote.contains("private final QuoteRepository repo;"));
        assertTrue(quote.contains("QuoteAdminView"));
        assertTrue(quote.contains("ServletRequestAttributes"));
        assertFalse(quote.contains("com.xinghuo.dto"));
        assertFalse(quote.contains("com.xinghuo.repository."));

        assertTrue(order.contains("private final CustomerOrderRepository repo;"));
        assertTrue(order.contains("AdminOrderView"));
        assertTrue(order.contains("ServletRequestAttributes"));
        assertFalse(order.contains("com.xinghuo.dto"));
        assertFalse(order.contains("com.xinghuo.repository."));

        assertTrue(catalog.contains("private final ServiceItemRepository repo;"));
        assertTrue(catalog.contains("ServiceItem item"));
        assertFalse(catalog.contains("com.xinghuo.model.ServiceItem"));
        assertTrue(catalog.contains("CatalogServiceCommand"));
        assertFalse(catalog.contains("com.xinghuo.dto"));
        assertFalse(catalog.contains("com.xinghuo.repository."));

        assertTrue(taxonomy.contains("private final GameCatalogRepository games;"));
        assertTrue(taxonomy.contains("private final ServiceCategoryCatalogRepository categories;"));
        assertTrue(taxonomy.contains("com.xinghuo.catalog.infrastructure.ServiceItemRepository"));
        assertTrue(taxonomy.contains("TaxonomyCommands"));
        assertFalse(taxonomy.contains("com.xinghuo.dto"));
        assertFalse(taxonomy.contains("com.xinghuo.repository."));

        assertTrue(dashboard.contains("CatalogMetricsApplicationService"));
        assertTrue(dashboard.contains("QuoteMetricsApplicationService"));
        assertTrue(dashboard.contains("OrderMetricsApplicationService"));
        assertTrue(dashboard.contains("TrackingApplicationService"));
        assertTrue(dashboard.contains("FeedbackApplicationService"));
        assertFalse(dashboard.contains("Repository"));
        assertFalse(dashboard.contains("com.xinghuo.dto"));
    }

    @Test
    void representativeApplicationContractsRemainExplicitTypedRecords() throws IOException {
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteSubmissionCommand.java")
                .contains("record QuoteSubmissionCommand"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteSubmissionResult.java")
                .contains("record QuoteSubmissionResult"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/OrderPublicView.java")
                .contains("record OrderPublicView"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/UpdateOrderCommand.java")
                .contains("record UpdateOrderCommand"));
        assertTrue(read("src/main/java/com/xinghuo/record/application/RecordPublicView.java")
                .contains("record RecordPublicView"));
        assertTrue(read("src/main/java/com/xinghuo/dashboard/application/DashboardView.java")
                .contains("record DashboardView"));
        assertTrue(read("src/main/java/com/xinghuo/taxonomy/application/TaxonomyCatalogView.java")
                .contains("record TaxonomyCatalogView"));
        assertTrue(read("src/main/java/com/xinghuo/catalog/application/CatalogServiceView.java")
                .contains("record CatalogServiceView"));
    }
}
