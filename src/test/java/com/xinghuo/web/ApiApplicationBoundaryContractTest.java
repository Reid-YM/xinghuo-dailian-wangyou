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
        for (String feature : new String[] {"quote", "order", "catalog", "record"}) {
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
        String recordMapper = read("src/main/java/com/xinghuo/record/web/RecordWebMapper.java");
        String catalogMapper = read("src/main/java/com/xinghuo/catalog/web/CatalogWebMapper.java");
        String catalogAdminMapper = read("src/main/java/com/xinghuo/catalog/web/CatalogAdminWebMapper.java");

        assertTrue(controller.contains("QuoteWebMapper.toCommand"));
        assertTrue(controller.contains("QuoteWebMapper.toResponse"));
        assertTrue(controller.contains("OrderWebMapper.toResponse"));
        assertTrue(controller.contains("OrderWebMapper.toCaseResponses"));
        assertTrue(controller.contains("RecordWebMapper.toResponse"));
        assertTrue(controller.contains("CatalogWebMapper.toResponses"));

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

        assertTrue(catalogMapper.contains("ApiDtos.ServiceResponse"));
        assertTrue(catalogAdminMapper.contains("ApiDtos.ServiceUpsert"));
        assertTrue(catalogAdminMapper.contains("ApiDtos.AdminServiceResponse"));
    }

    @Test
    void infrastructureContainsPersistenceTranslationWhileProtocolDtosStayAtWebBoundary() throws IOException {
        String quote = read("src/main/java/com/xinghuo/quote/infrastructure/QuotePersistenceAdapter.java");
        String order = read("src/main/java/com/xinghuo/order/infrastructure/OrderPersistenceAdapter.java");
        String record = read("src/main/java/com/xinghuo/record/infrastructure/RecordPersistenceAdapter.java");
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
