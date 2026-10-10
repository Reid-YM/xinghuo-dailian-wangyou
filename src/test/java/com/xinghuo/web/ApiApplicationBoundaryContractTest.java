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
        for (String feature : new String[] {"quote", "order", "catalog", "record", "dashboard", "feedback", "taxonomy"}) {
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
        String publicController = read("src/main/java/com/xinghuo/controller/PublicApiController.java");
        String publicQuote = read("src/main/java/com/xinghuo/quote/web/PublicQuoteController.java");
        String publicRecord = read("src/main/java/com/xinghuo/record/web/PublicRecordController.java");
        String publicOrder = read("src/main/java/com/xinghuo/order/web/PublicOrderController.java");
        String publicServices = read("src/main/java/com/xinghuo/catalog/web/PublicCatalogServiceController.java");
        String publicCatalog = read("src/main/java/com/xinghuo/taxonomy/web/PublicTaxonomyCatalogController.java");
        String publicOrderCases = read("src/main/java/com/xinghuo/order/web/PublicOrderCaseController.java");
        String adminController = read("src/main/java/com/xinghuo/controller/AdminApiController.java");
        String quoteMapper = read("src/main/java/com/xinghuo/quote/web/QuoteWebMapper.java");
        String orderMapper = read("src/main/java/com/xinghuo/order/web/OrderWebMapper.java");
        String recordMapper = read("src/main/java/com/xinghuo/record/web/RecordWebMapper.java");
        String dashboardMapper = read("src/main/java/com/xinghuo/dashboard/web/DashboardWebMapper.java");
        String feedbackMapper = read("src/main/java/com/xinghuo/feedback/web/FeedbackWebMapper.java");
        String taxonomyMapper = read("src/main/java/com/xinghuo/taxonomy/web/TaxonomyWebMapper.java");
        String catalogMapper = read("src/main/java/com/xinghuo/catalog/web/CatalogWebMapper.java");
        String catalogAdminMapper = read("src/main/java/com/xinghuo/catalog/web/CatalogAdminWebMapper.java");

        assertTrue(publicQuote.contains("QuoteWebMapper.toCommand"));
        assertTrue(publicQuote.contains("QuoteWebMapper.toResponse"));
        assertTrue(publicOrder.contains("OrderWebMapper.toResponse"));
        assertTrue(publicOrderCases.contains("OrderWebMapper.toCaseResponses"));
        assertTrue(publicRecord.contains("RecordWebMapper.toResponse"));
        assertTrue(publicServices.contains("CatalogWebMapper.toResponses"));
        assertTrue(publicCatalog.contains("TaxonomyWebMapper.toResponse"));
        assertTrue(adminController.contains("DashboardWebMapper.toResponse"));
        assertTrue(adminController.contains("FeedbackWebMapper.toCommand"));
        assertTrue(adminController.contains("FeedbackWebMapper.toResponse"));
        assertTrue(adminController.contains("TaxonomyWebMapper.toCommand"));
        assertTrue(adminController.contains("TaxonomyWebMapper.toResponse"));

        assertTrue(quoteMapper.contains("PublicQuoteRequest"));
        assertTrue(quoteMapper.contains("ApiDtos.QuoteResponse"));
        assertTrue(quoteMapper.contains("ApiDtos.QuoteAdminResponse"));
        assertTrue(quoteMapper.contains("QuoteLeadDtos.QuoteLeadResponse"));
        assertTrue(quoteMapper.contains("QuoteStatusUpdateCommand"));
        assertTrue(quoteMapper.contains("QuoteCrmUpdateCommand"));
        assertTrue(quoteMapper.contains("QuoteConvertCommand"));

        assertTrue(orderMapper.contains("ApiDtos.OrderPublicResponse"));
        assertTrue(orderMapper.contains("ApiDtos.AdminOrderResponse"));
        assertTrue(orderMapper.contains("CreateOrderCommand"));
        assertTrue(orderMapper.contains("UpdateOrderCommand"));

        assertTrue(recordMapper.contains("ApiDtos.RecordPublicResponse"));
        assertTrue(recordMapper.contains("RecordPublicView"));

        assertTrue(dashboardMapper.contains("ApiDtos.DashboardResponse"));
        assertTrue(dashboardMapper.contains("DashboardView"));

        assertTrue(feedbackMapper.contains("ApiDtos.TestFeedbackCreateRequest"));
        assertTrue(feedbackMapper.contains("ApiDtos.TestFeedbackUpdateRequest"));
        assertTrue(feedbackMapper.contains("ApiDtos.TestFeedbackResponse"));
        assertTrue(feedbackMapper.contains("CreateTestFeedbackCommand"));
        assertTrue(feedbackMapper.contains("UpdateTestFeedbackCommand"));
        assertTrue(feedbackMapper.contains("TestFeedbackView"));

        assertTrue(taxonomyMapper.contains("ApiDtos.CatalogResponse"));
        assertTrue(taxonomyMapper.contains("ApiDtos.CatalogNameRequest"));
        assertTrue(taxonomyMapper.contains("ApiDtos.GameProfileUpdateRequest"));
        assertTrue(taxonomyMapper.contains("TaxonomyCatalogView"));
        assertTrue(taxonomyMapper.contains("TaxonomyCommands"));

        assertTrue(catalogMapper.contains("ApiDtos.ServiceResponse"));
        assertTrue(catalogAdminMapper.contains("ApiDtos.ServiceUpsert"));
        assertTrue(catalogAdminMapper.contains("ApiDtos.AdminServiceResponse"));
    }

    @Test
    void infrastructureContainsPersistenceTranslationWhileProtocolDtosStayAtWebBoundary() throws IOException {
        String quote = read("src/main/java/com/xinghuo/quote/infrastructure/QuotePersistenceAdapter.java");
        String order = read("src/main/java/com/xinghuo/order/infrastructure/OrderPersistenceAdapter.java");
        String record = read("src/main/java/com/xinghuo/record/infrastructure/RecordPersistenceAdapter.java");
        String dashboard = read("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java");
        String feedback = read("src/main/java/com/xinghuo/feedback/infrastructure/FeedbackPersistenceAdapter.java");
        String taxonomy = read("src/main/java/com/xinghuo/taxonomy/infrastructure/TaxonomyPersistenceAdapter.java");
        String catalog = read("src/main/java/com/xinghuo/catalog/infrastructure/CatalogPersistenceAdapter.java");

        assertTrue(quote.contains("com.xinghuo.repository.QuoteRepository"));
        assertTrue(quote.contains("QuoteAdminView"));
        assertTrue(quote.contains("QuoteLeadView"));
        assertTrue(quote.contains("ServletRequestAttributes"));
        assertFalse(quote.contains("com.xinghuo.dto"));

        assertTrue(order.contains("com.xinghuo.repository.CustomerOrderRepository"));
        assertTrue(order.contains("AdminOrderView"));
        assertTrue(order.contains("OrderPublicView"));
        assertTrue(order.contains("ServletRequestAttributes"));
        assertFalse(order.contains("com.xinghuo.dto"));

        assertTrue(record.contains("com.xinghuo.repository.CustomerOrderRepository"));
        assertTrue(record.contains("com.xinghuo.repository.QuoteRepository"));
        assertTrue(record.contains("RecordPublicView"));
        assertFalse(record.contains("com.xinghuo.dto"));

        assertTrue(dashboard.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertTrue(dashboard.contains("com.xinghuo.repository.AdminTestFeedbackRepository"));
        assertTrue(dashboard.contains("DashboardView"));
        assertFalse(dashboard.contains("com.xinghuo.dto"));
        assertFalse(dashboard.contains("com.xinghuo.service"));

        assertTrue(feedback.contains("com.xinghuo.repository.AdminTestFeedbackRepository"));
        assertTrue(feedback.contains("com.xinghuo.audit.infrastructure.AuditService"));
        assertTrue(feedback.contains("TestFeedbackView"));
        assertTrue(feedback.contains("ServletRequestAttributes"));
        assertFalse(feedback.contains("com.xinghuo.dto"));
        assertFalse(feedback.contains("AdminTestFeedbackService"));

        assertTrue(taxonomy.contains("com.xinghuo.repository.GameCatalogRepository"));
        assertTrue(taxonomy.contains("com.xinghuo.repository.ServiceCategoryCatalogRepository"));
        assertTrue(taxonomy.contains("com.xinghuo.repository.PlatformCatalogRepository"));
        assertTrue(taxonomy.contains("com.xinghuo.repository.GamePlatformRepository"));
        assertTrue(taxonomy.contains("TaxonomyCatalogView"));
        assertTrue(taxonomy.contains("TaxonomyCommands"));
        assertTrue(taxonomy.contains("ServletRequestAttributes"));
        assertFalse(taxonomy.contains("com.xinghuo.dto"));
        assertFalse(taxonomy.contains("TaxonomyService"));

        assertTrue(catalog.contains("com.xinghuo.model.ServiceItem"));
        assertTrue(catalog.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertTrue(catalog.contains("CatalogPageServiceView"));
        assertTrue(catalog.contains("CatalogServiceCommand"));
        assertTrue(catalog.contains("ServletRequestAttributes"));
        assertFalse(catalog.contains("com.xinghuo.dto"));
    }

    @Test
    void applicationContractsAreExplicitTypedRecords() throws IOException {
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteSubmissionCommand.java")
                .contains("record QuoteSubmissionCommand"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteSubmissionResult.java")
                .contains("record QuoteSubmissionResult"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteAdminView.java")
                .contains("record QuoteAdminView"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteLeadView.java")
                .contains("record QuoteLeadView"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteCrmUpdateCommand.java")
                .contains("record QuoteCrmUpdateCommand"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteStatusUpdateCommand.java")
                .contains("record QuoteStatusUpdateCommand"));
        assertTrue(read("src/main/java/com/xinghuo/quote/application/QuoteConvertCommand.java")
                .contains("record QuoteConvertCommand"));

        assertTrue(read("src/main/java/com/xinghuo/order/application/OrderPublicView.java")
                .contains("record OrderPublicView"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/PublicCaseView.java")
                .contains("record PublicCaseView"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/AdminOrderView.java")
                .contains("record AdminOrderView"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/CreateOrderCommand.java")
                .contains("record CreateOrderCommand"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/UpdateOrderCommand.java")
                .contains("record UpdateOrderCommand"));
        assertTrue(read("src/main/java/com/xinghuo/order/application/OrderFromQuoteCommand.java")
                .contains("record OrderFromQuoteCommand"));

        assertTrue(read("src/main/java/com/xinghuo/record/application/RecordPublicView.java")
                .contains("record RecordPublicView"));

        assertTrue(read("src/main/java/com/xinghuo/dashboard/application/DashboardView.java")
                .contains("record DashboardView"));

        assertTrue(read("src/main/java/com/xinghuo/feedback/application/TestFeedbackView.java")
                .contains("record TestFeedbackView"));
        assertTrue(read("src/main/java/com/xinghuo/feedback/application/CreateTestFeedbackCommand.java")
                .contains("record CreateTestFeedbackCommand"));
        assertTrue(read("src/main/java/com/xinghuo/feedback/application/UpdateTestFeedbackCommand.java")
                .contains("record UpdateTestFeedbackCommand"));

        assertTrue(read("src/main/java/com/xinghuo/taxonomy/application/TaxonomyCatalogView.java")
                .contains("record TaxonomyCatalogView"));
        assertTrue(read("src/main/java/com/xinghuo/taxonomy/application/TaxonomyCatalogView.java")
                .contains("record GameView"));
        assertTrue(read("src/main/java/com/xinghuo/taxonomy/application/TaxonomyCommands.java")
                .contains("record CreateGame"));
        assertTrue(read("src/main/java/com/xinghuo/taxonomy/application/TaxonomyCommands.java")
                .contains("record UpdateGameProfile"));
        assertTrue(read("src/main/java/com/xinghuo/taxonomy/application/TaxonomyCommands.java")
                .contains("record CreateCategory"));

        assertTrue(read("src/main/java/com/xinghuo/catalog/application/CatalogServiceView.java")
                .contains("record CatalogServiceView"));
        assertTrue(read("src/main/java/com/xinghuo/catalog/application/CatalogServiceCommand.java")
                .contains("record CatalogServiceCommand"));
        assertTrue(read("src/main/java/com/xinghuo/catalog/application/CatalogAdminServiceView.java")
                .contains("record CatalogAdminServiceView"));
        String pageView = read("src/main/java/com/xinghuo/catalog/application/CatalogPageServiceView.java");
        assertTrue(pageView.contains("record CatalogPageServiceView"));
        assertTrue(pageView.contains("enum PriceMode"));
        assertTrue(pageView.contains("enum Status"));
        assertTrue(pageView.contains("getPriceMode()"));
        assertTrue(pageView.contains("getStatus()"));
    }
}
