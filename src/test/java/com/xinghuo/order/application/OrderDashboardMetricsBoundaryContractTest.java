package com.xinghuo.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OrderDashboardMetricsBoundaryContractTest {
    @Test
    void dashboardReadsOrderCountsThroughApplicationBoundary() throws Exception {
        String dashboard = Files.readString(Path.of("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java"));

        assertThat(dashboard)
                .contains("OrderMetricsApplicationService")
                .contains("orders.countInProgress()")
                .contains("orders.countCompleted()")
                .doesNotContain("CustomerOrderRepository")
                .doesNotContain("com.xinghuo.model.CustomerOrder");
    }
}
