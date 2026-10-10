package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminApiRouteOwnershipContractTest {
    @Test
    void adminRoutesAreOwnedByFeatureWebPackages() {
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/dashboard/web/AdminDashboardController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/catalog/web/AdminCatalogServiceController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/taxonomy/web/AdminTaxonomyController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/quote/web/AdminQuoteController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/order/web/AdminOrderController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/siteconfig/web/AdminSiteConfigController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/feedback/web/AdminFeedbackController.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/xinghuo/audit/web/AdminAuditController.java"))).isTrue();
    }
}
