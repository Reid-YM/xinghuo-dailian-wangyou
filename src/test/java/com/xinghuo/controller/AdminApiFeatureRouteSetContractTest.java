package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminApiFeatureRouteSetContractTest {
    @Test
    void representativeFormerRoutesRemainPresentAfterSplit() throws Exception {
        String all = Files.readString(Path.of("src/main/java/com/xinghuo/dashboard/web/AdminDashboardController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/siteconfig/web/AdminSiteConfigController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/gamestatus/web/AdminGameStatusController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/catalog/web/AdminCatalogServiceController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/taxonomy/web/AdminTaxonomyController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/toolstation/web/AdminToolStationController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/quote/web/AdminQuoteController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/order/web/AdminOrderController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/slot/web/AdminSlotController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/feedback/web/AdminFeedbackController.java"))
                + Files.readString(Path.of("src/main/java/com/xinghuo/audit/web/AdminAuditController.java"));

        assertThat(all)
                .contains("/api/admin")
                .contains("/dashboard")
                .contains("/site-config")
                .contains("/game-status")
                .contains("/services")
                .contains("/catalog")
                .contains("/tool-station")
                .contains("/quotes")
                .contains("/orders")
                .contains("/slots")
                .contains("/test-feedback")
                .contains("/audits");
    }
}
