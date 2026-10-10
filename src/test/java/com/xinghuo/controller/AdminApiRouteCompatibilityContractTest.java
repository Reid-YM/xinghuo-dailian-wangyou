package com.xinghuo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminApiRouteCompatibilityContractTest {
    @Test
    void featureControllersKeepExistingAdminRoutePrefixes() throws Exception {
        assertThat(Files.readString(Path.of("src/main/java/com/xinghuo/order/web/AdminOrderController.java")))
                .contains("/api/admin/orders");
        assertThat(Files.readString(Path.of("src/main/java/com/xinghuo/quote/web/AdminQuoteController.java")))
                .contains("/api/admin/quotes");
        assertThat(Files.readString(Path.of("src/main/java/com/xinghuo/catalog/web/AdminCatalogServiceController.java")))
                .contains("/api/admin/services");
        assertThat(Files.readString(Path.of("src/main/java/com/xinghuo/taxonomy/web/AdminTaxonomyController.java")))
                .contains("/api/admin/catalog");
        assertThat(Files.readString(Path.of("src/main/java/com/xinghuo/siteconfig/web/AdminSiteConfigController.java")))
                .contains("/api/admin/site-config");
    }
}
