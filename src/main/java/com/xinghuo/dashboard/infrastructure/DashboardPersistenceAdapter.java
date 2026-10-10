package com.xinghuo.dashboard.infrastructure;

import com.xinghuo.dashboard.application.DashboardPort;
import com.xinghuo.dashboard.application.DashboardView;
import com.xinghuo.model.AdminTestFeedback;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.AdminTestFeedbackRepository;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.repository.TrackingEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dashboard-owned cross-feature read adapter. */
@Component
public class DashboardPersistenceAdapter implements DashboardPort {
    private final ServiceItemRepository services;
    private final QuoteRepository quotes;
    private final CustomerOrderRepository orders;
    private final TrackingEventRepository events;
    private final AdminTestFeedbackRepository feedback;

    public DashboardPersistenceAdapter(
            ServiceItemRepository services,
            QuoteRepository quotes,
            CustomerOrderRepository orders,
            TrackingEventRepository events,
            AdminTestFeedbackRepository feedback) {
        this.services = services;
        this.quotes = quotes;
        this.orders = orders;
        this.events = events;
        this.feedback = feedback;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView get() {
        return new DashboardView(
                services.count(),
                quotes.countByStatus(Quote.Status.NEW),
                orders.countByStatus(CustomerOrder.Status.IN_PROGRESS),
                orders.countByStatus(CustomerOrder.Status.COMPLETED),
                events.count(),
                feedback.countByStatus(AdminTestFeedback.Status.NEW));
    }
}
