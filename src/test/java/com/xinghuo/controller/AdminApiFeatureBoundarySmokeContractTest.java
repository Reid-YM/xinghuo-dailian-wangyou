package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminApiFeatureBoundarySmokeContractTest {
    @Test
    void allFormerAdminApiResponsibilitiesHaveFeatureControllers() {
        List<String> paths = List.of(
                "src/main/java/com/xinghuo/dashboard/web/AdminDashboardController.java",
                "src/main/java/com/xinghuo/siteconfig/web/AdminSiteConfigController.java",
                "src/main/java/com/xinghuo/gamestatus/web/AdminGameStatusController.java",
                "src/main/java/com/xinghuo/catalog/web/AdminCatalogServiceController.java",
                "src/main/java/com/xinghuo/taxonomy/web/AdminTaxonomyController.java",
                "src/main/java/com/xinghuo/toolstation/web/AdminToolStationController.java",
                "src/main/java/com/xinghuo/quote/web/AdminQuoteController.java",
                "src/main/java/com/xinghuo/order/web/AdminOrderController.java",
                "src/main/java/com/xinghuo/slot/web/AdminSlotController.java",
                "src/main/java/com/xinghuo/feedback/web/AdminFeedbackController.java",
                "src/main/java/com/xinghuo/audit/web/AdminAuditController.java");

        assertThat(paths).allSatisfy(path -> assertThat(Files.exists(Path.of(path))).isTrue());
    }
}
