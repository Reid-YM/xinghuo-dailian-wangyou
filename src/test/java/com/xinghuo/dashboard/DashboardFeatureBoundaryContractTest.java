package com.xinghuo.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DashboardFeatureBoundaryContractTest {
    @Test
    void dashboardDependsOnlyOnFeatureApplicationMetrics() throws Exception {
        String dashboard = Files.readString(Path.of("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java"));

        assertThat(dashboard)
                .contains("CatalogMetricsApplicationService")
                .contains("QuoteMetricsApplicationService")
                .contains("OrderMetricsApplicationService")
                .contains("TrackingApplicationService")
                .contains("FeedbackApplicationService")
                .doesNotContain("com.xinghuo.repository")
                .doesNotContain("com.xinghuo.model");
    }
}
