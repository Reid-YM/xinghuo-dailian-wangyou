package com.xinghuo.dashboard.infrastructure;

import com.xinghuo.catalog.application.CatalogMetricsApplicationService;
import com.xinghuo.dashboard.application.DashboardPort;
import com.xinghuo.dashboard.application.DashboardView;
import com.xinghuo.feedback.application.FeedbackApplicationService;
import com.xinghuo.order.application.OrderMetricsApplicationService;
import com.xinghuo.quote.application.QuoteMetricsApplicationService;
import com.xinghuo.tracking.application.TrackingApplicationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard-owned cross-feature read adapter. */
@Component
public class DashboardPersistenceAdapter implements DashboardPort {
    private final CatalogMetricsApplicationService catalog;
    private final QuoteMetricsApplicationService quotes;
    private final OrderMetricsApplicationService orders;
    private final TrackingApplicationService tracking;
    private final FeedbackApplicationService feedback;

    public DashboardPersistenceAdapter(
            CatalogMetricsApplicationService catalog,
            QuoteMetricsApplicationService quotes,
            OrderMetricsApplicationService orders,
            TrackingApplicationService tracking,
            FeedbackApplicationService feedback) {
        this.catalog = catalog;
        this.quotes = quotes;
        this.orders = orders;
        this.tracking = tracking;
        this.feedback = feedback;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView get() {
        return new DashboardView(
                catalog.totalServices(),
                quotes.countNew(),
                orders.countInProgress(),
                orders.countCompleted(),
                tracking.totalCount(),
                feedback.countNew());
    }
}
