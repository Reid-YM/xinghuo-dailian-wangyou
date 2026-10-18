package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.repository.TrackingEventRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final ServiceItemRepository services;
    private final QuoteRepository quotes;
    private final CustomerOrderRepository orders;
    private final TrackingEventRepository events;
    private final AdminTestFeedbackService feedback;

    public DashboardService(
            ServiceItemRepository services,
            QuoteRepository quotes,
            CustomerOrderRepository orders,
            TrackingEventRepository events,
            AdminTestFeedbackService feedback) {
        this.services = services;
        this.quotes = quotes;
        this.orders = orders;
        this.events = events;
        this.feedback = feedback;
    }

    public ApiDtos.DashboardResponse get() {
        return new ApiDtos.DashboardResponse(
                this.services.count(),
                this.quotes.countByStatus(Quote.Status.NEW),
                this.orders.countByStatus(CustomerOrder.Status.IN_PROGRESS),
                this.orders.countByStatus(CustomerOrder.Status.COMPLETED)
                        + this.orders.countByStatus(CustomerOrder.Status.CLOSED),
                this.events.count(),
                this.feedback.newCount());
    }
}
