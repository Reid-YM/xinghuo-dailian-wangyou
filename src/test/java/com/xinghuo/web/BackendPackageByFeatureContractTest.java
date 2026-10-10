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
    void featureModulesHaveExplicitBoundaries() {
        for (String feature : new String[] {"quote", "order", "catalog", "record", "dashboard", "feedback", "taxonomy"}) {
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
        assertTrue(controller.contains("com.xinghuo.record.application.RecordApplicationService"));
        assertTrue(controller.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertTrue(controller.contains("TaxonomyWebMapper.toResponse"));
        assertTrue(controller.contains("RecordWebMapper.toResponse"));
        assertTrue(controller.contains("OrderProgressAsset"));

        assertFalse(controller.contains("import com.xinghuo.service.QuoteService;"));
        assertFalse(controller.contains("import com.xinghuo.service.OrderService;"));
        assertFalse(controller.contains("import com.xinghuo.service.ServiceCatalogService;"));
        assertFalse(controller.contains("import com.xinghuo.service.RecordService;"));
        assertFalse(controller.contains("import com.xinghuo.service.TaxonomyService;"));
        assertFalse(controller.contains("import com.xinghuo.model.CustomerOrder;"));
    }

    @Test
    void adminDashboardConsumesApplicationBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/AdminApiController.java");

        assertTrue(controller.contains("com.xinghuo.dashboard.application.DashboardApplicationService"));
        assertTrue(controller.contains("DashboardWebMapper.toResponse"));
        assertFalse(controller.contains("import com.xinghuo.service.DashboardService;"));
    }

    @Test
    void adminFeedbackConsumesApplicationBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/AdminApiController.java");

        assertTrue(controller.contains("com.xinghuo.feedback.application.FeedbackApplicationService"));
        assertTrue(controller.contains("FeedbackWebMapper.toCommand"));
        assertTrue(controller.contains("FeedbackWebMapper.toResponse"));
        assertTrue(controller.contains("FeedbackWebMapper.toResponses"));
        assertFalse(controller.contains("import com.xinghuo.service.AdminTestFeedbackService;"));
    }

    @Test
    void adminTaxonomyConsumesApplicationBoundary() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/AdminApiController.java");

        assertTrue(controller.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertTrue(controller.contains("TaxonomyWebMapper.toCommand"));
        assertTrue(controller.contains("TaxonomyWebMapper.toResponse"));
        assertTrue(controller.contains("TaxonomyWebMapper.toGameResponses"));
        assertFalse(controller.contains("import com.xinghuo.service.TaxonomyService;"));
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
    void adminOrderFlowsConsumeOrderApplicationBoundary() throws IOException {
        String admin = read("src/main/java/com/xinghuo/controller/AdminApiController.java");

        assertTrue(admin.contains("com.xinghuo.order.application.OrderApplicationService"));
        assertTrue(admin.contains("OrderWebMapper.toCommand"));
        assertTrue(admin.contains("OrderWebMapper.toAdminResponses"));
        assertFalse(admin.contains("import com.xinghuo.service.OrderService;"));
    }

    @Test
    void pageControllerConsumesApplicationProjections() throws IOException {
        String controller = read("src/main/java/com/xinghuo/controller/PageController.java");

        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogApplicationService"));
        assertTrue(controller.contains("com.xinghuo.catalog.application.CatalogPageServiceView"));
        assertTrue(controller.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertTrue(controller.contains("com.xinghuo.taxonomy.application.TaxonomyCatalogView"));
        assertTrue(controller.contains("publicPageItems()"));
        assertTrue(controller.contains("publicPageBySlug(slug)"));
        assertFalse(controller.contains("import com.xinghuo.service.ServiceCatalogService;"));
        assertFalse(controller.contains("import com.xinghuo.service.TaxonomyService;"));
        assertFalse(controller.contains("import com.xinghuo.model.ServiceItem;"));
        assertFalse(controller.contains("import com.xinghuo.model.GameCatalog;"));
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
                "src/main/java/com/xinghuo/catalog/application/CatalogApplicationService.java",
                "src/main/java/com/xinghuo/record/application/RecordApplicationService.java",
                "src/main/java/com/xinghuo/dashboard/application/DashboardApplicationService.java",
                "src/main/java/com/xinghuo/feedback/application/FeedbackApplicationService.java",
                "src/main/java/com/xinghuo/taxonomy/application/TaxonomyApplicationService.java"
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
                "src/main/java/com/xinghuo/catalog/domain/package-info.java",
                "src/main/java/com/xinghuo/record/domain/package-info.java",
                "src/main/java/com/xinghuo/dashboard/domain/package-info.java",
                "src/main/java/com/xinghuo/feedback/domain/package-info.java",
                "src/main/java/com/xinghuo/taxonomy/domain/package-info.java"
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
    void retiredLegacyServicesStayRemovedAndOwnedPersistenceAdaptersAreUsed() throws IOException {
        String quote = read("src/main/java/com/xinghuo/quote/infrastructure/QuotePersistenceAdapter.java");
        assertTrue(quote.contains("implements QuotePort"));
        assertTrue(quote.contains("com.xinghuo.repository.QuoteRepository"));
        assertTrue(quote.contains("com.xinghuo.order.application.OrderApplicationService"));
        assertFalse(quote.contains("com.xinghuo.service.QuoteService"));
        assertFalse(quote.contains("com.xinghuo.service.OrderService"));

        String order = read("src/main/java/com/xinghuo/order/infrastructure/OrderPersistenceAdapter.java");
        assertTrue(order.contains("implements OrderPort"));
        assertTrue(order.contains("com.xinghuo.repository.CustomerOrderRepository"));
        assertFalse(order.contains("com.xinghuo.service.OrderService"));

        String catalog = read("src/main/java/com/xinghuo/catalog/infrastructure/CatalogPersistenceAdapter.java");
        assertTrue(catalog.contains("implements CatalogPort"));
        assertTrue(catalog.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertTrue(catalog.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertFalse(catalog.contains("com.xinghuo.service.ServiceCatalogService"));
        assertFalse(catalog.contains("com.xinghuo.service.TaxonomyService"));

        String record = read("src/main/java/com/xinghuo/record/infrastructure/RecordPersistenceAdapter.java");
        assertTrue(record.contains("implements RecordPort"));
        assertTrue(record.contains("com.xinghuo.repository.CustomerOrderRepository"));
        assertTrue(record.contains("com.xinghuo.repository.QuoteRepository"));
        assertFalse(record.contains("com.xinghuo.service.RecordService"));

        String dashboard = read("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java");
        assertTrue(dashboard.contains("implements DashboardPort"));
        assertTrue(dashboard.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertTrue(dashboard.contains("com.xinghuo.repository.AdminTestFeedbackRepository"));
        assertFalse(dashboard.contains("com.xinghuo.service.DashboardService"));
        assertFalse(dashboard.contains("com.xinghuo.service.AdminTestFeedbackService"));

        String feedback = read("src/main/java/com/xinghuo/feedback/infrastructure/FeedbackPersistenceAdapter.java");
        assertTrue(feedback.contains("implements FeedbackPort"));
        assertTrue(feedback.contains("com.xinghuo.repository.AdminTestFeedbackRepository"));
        assertTrue(feedback.contains("com.xinghuo.service.AuditService"));
        assertFalse(feedback.contains("com.xinghuo.service.AdminTestFeedbackService"));
        assertFalse(feedback.contains("com.xinghuo.dto"));

        String taxonomy = read("src/main/java/com/xinghuo/taxonomy/infrastructure/TaxonomyPersistenceAdapter.java");
        assertTrue(taxonomy.contains("implements TaxonomyPort"));
        assertTrue(taxonomy.contains("com.xinghuo.repository.GameCatalogRepository"));
        assertTrue(taxonomy.contains("com.xinghuo.repository.ServiceCategoryCatalogRepository"));
        assertTrue(taxonomy.contains("com.xinghuo.repository.PlatformCatalogRepository"));
        assertTrue(taxonomy.contains("com.xinghuo.repository.GamePlatformRepository"));
        assertTrue(taxonomy.contains("com.xinghuo.service.AuditService"));
        assertFalse(taxonomy.contains("com.xinghuo.service.TaxonomyService"));
        assertFalse(taxonomy.contains("com.xinghuo.dto"));

        String slot = read("src/main/java/com/xinghuo/slot/infrastructure/SlotPersistenceAdapter.java");
        assertTrue(slot.contains("implements SlotPort"));
        assertTrue(slot.contains("com.xinghuo.repository.HotSlotRepository"));
        assertTrue(slot.contains("com.xinghuo.repository.ServiceItemRepository"));
        assertTrue(slot.contains("com.xinghuo.taxonomy.application.TaxonomyApplicationService"));
        assertFalse(slot.contains("com.xinghuo.service.SlotService"));
        assertFalse(slot.contains("com.xinghuo.service.TaxonomyService"));
    }
}
