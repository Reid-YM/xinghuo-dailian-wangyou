package com.xinghuo.quote.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class QuoteDashboardMetricsBoundaryContractTest {
    @Test
    void dashboardReadsQuoteCountThroughApplicationBoundary() throws Exception {
        String dashboard = Files.readString(Path.of("src/main/java/com/xinghuo/dashboard/infrastructure/DashboardPersistenceAdapter.java"));

        assertThat(dashboard)
                .contains("QuoteMetricsApplicationService")
                .contains("quotes.countNew()")
                .doesNotContain("QuoteRepository")
                .doesNotContain("com.xinghuo.model.Quote");
    }
}
