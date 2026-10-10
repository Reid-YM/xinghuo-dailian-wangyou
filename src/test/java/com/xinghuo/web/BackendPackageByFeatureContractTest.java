package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BackendPackageByFeatureContractTest {
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    @Test
    void quoteOrderAndCatalogHaveFeatureBoundaries() {
        for (String feature : new String[] {"quote", "order", "catalog"}) {
            Path base = ROOT.resolve("src/main/java/com/xinghuo/" + feature);
            assertTrue(Files.isDirectory(base.resolve("application")), feature + " application boundary must exist");
            assertTrue(Files.isDirectory(base.resolve("domain")), feature + " domain boundary must exist");
            assertTrue(Files.isDirectory(base.resolve("infrastructure")), feature + " infrastructure boundary must exist");
            assertTrue(Files.isDirectory(base.resolve("web")), feature + " web boundary must exist");
        }
    }

    @Test
    void publicApiConsumesFeatureApplicationServices() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/PublicApiController.java");

        assertTrue(controller.contains("com.xinghuo.quote.application.QuoteApplicationService"));
        assertTrue(controller.contains("com.xinghuo.order.application.OrderApplicationService"));
        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogApplicationService"));
        assertTrue(controller.contains("OrderProgressAsset"));

        assertFalse(controller.contains("import com.xinghuo.service.QuoteService;"));
        assertFalse(controller.contains("import com.xinghuo.service.OrderService;"));
        assertFalse(controller.contains("import com.xinghuo.service.ServiceCatalogService;"));
        assertFalse(controller.contains("import com.xinghuo.model.CustomerOrder;"));
    }

    @Test
    void adminServiceCrudConsumesCatalogApplicationBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/AdminApiController.java");

        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogApplicationService"));
        assertTrue(controller.contains("CatalogAdminWebMapper.toCommand"));
        assertTrue(controller.contains("CatalogAdminWebMapper.toResponse"));
        assertTrue(controller.contains("CatalogAdminWebMapper.toResponses"));
        assertFalse(controller.contains("import com.xinghuo.service.ServiceCatalogService;"));
    }

    @Test
    void adminQuoteFlowsConsumeQuoteApplicationBoundary() throws IOException {
        String admin = read("src/main/java/com/xinghuo/controller/AdminApiController.java");
        String leads = read("src/main/java/com/xinghuo/controller/AdminLeadController.java");

        assertTrue(admin.contains("com.xinghuo.quote.application.QuoteApplicationService"));
        assertTrue(leads.contains("com.xinghuo.quote.application.QuoteApplicationService"));
        assertFalse(admin.contains("import com.xinghuo.service.QuoteService;"));
        assertFalse(leads.contains("import com.xinghuo.service.QuoteService;"));
    }

    @Test
    void pageControllerConsumesCatalogApplicationProjection() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/PageController.java");

        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogApplicationService"));
        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogPageServiceView"));
        assertTrue(controller.contains("publicPageItems()"));
        assertTrue(controller.contains("publicPageBySlug(slug)"));
        assertFalse(controller.contains("import com.xinghuo.service.ServiceCatalogService;"));
        assertFalse(controller.contains("import com.xinghuo.model.ServiceItem;"));
    }

    @Test
    void seoControllerConsumesCatalogApplicationProjection() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/SeoController.java");

        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogApplicationService"));
        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogPageServiceView"));
        assertTrue(controller.contains("publicPageItems()"));
        assertFalse(controller.contains("import com.xinghuo.service.ServiceCatalogService;"));
        assertFalse(controller.contains("import com.xinghuo.model.ServiceItem;"));
    }

    @Test
    void applicationLayerDoesNotReachIntoLegacyPersistence() throws IOException {
        for (String file : new String[] {
                "src/main/java/com/xinghuo/quote/application/QuoteApplicationService.java",
                "src/main/java/com/xinghuo/order/application/OrderApplicationService.java",
                "src/main/java/com/xinghuo/catalog/application/CatalogApplicationService.java"
        }) {
            String source = read(file);
            assertFalse(source.contains("com.xinghuo.repository."), file + " must not depend on legacy repositories");
            assertFalse(source.contains("com.xinghuo.service."), file + " must not depend on legacy services");
            assertFalse(source.contains("jakarta.persistence"), file + " must not depend on JPA");
            assertFalse(source.contains("jakarta.servlet"), file + " must not depend on servlet APIs");
        }
    }

    @Test
    void domainLayerRemainsFrameworkIndependent() throws IOException {
        for (String file : new String[] {
                "src/main/java/com/xinghuo/quote/domain/package-info.java",
                "src/main/java/com/xinghuo/order/domain/OrderProgressAsset.java",
                "src/main/java/com/xinghuo/catalog/domain/package-info.java"
        }) {
            String source = read(file);
            assertFalse(source.contains("org.springframework"), file + " must stay framework independent");
            assertFalse(source.contains("jakarta.persistence"), file + " must stay persistence independent");
            assertFalse(source.contains("jakarta.servlet"), file + " must stay web independent");
            assertFalse(source.contains("com.xinghuo.repository"), file + " must not depend on legacy repositories");
            assertFalse(source.contains("com.xinghuo.service"), file + " must not depend on legacy services");
        }
    }

    @Test
    void remainingLegacyServicesAreConfinedAndOwnedPersistenceAdaptersAreUsed() throws IOException {
        String quote = read("src/main/java/com/xinghuo/quote/infrastructure/QuotePersistenceAdapter.java");
        assertTrue(quote.contains("implements QuotePort"));
        assertTrue(quote.contains("com.xinghuo.repository.QuoteRepository"));
        assertFalse(quote.contains("com.xinghuo.service.QuoteService"));

        assertTrue(read("src/main/java/com/xinghuo/order/infrastructure/LegacyOrderAdapter.java")
                .contains("com.xinghuo.service.OrderService"));

        String catalog = read("src/main/java/com/xinghuo/catalog/infrastructure/CatalogPersistenceAdapter.java");
        assertTrue(catalog.contains("implements CatalogPort"));
        assertTrue(catalog.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertFalse(catalog.contains("com.xinghuo.service.ServiceCatalogService"));
    }
}
