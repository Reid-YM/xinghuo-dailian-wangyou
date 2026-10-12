package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.model.Quote;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.repository.ServiceItemRepository;
import com.xinghuo.repository.TrackingEventRepository;
import org.junit.jupiter.api.Test;

class DashboardServiceCharacterizationTest {

    @Test
    void dashboardMapsRepositoryAndFeedbackCountsInResponseOrder() {
        ServiceItemRepository services = mock(ServiceItemRepository.class);
        QuoteRepository quotes = mock(QuoteRepository.class);
        CustomerOrderRepository orders = mock(CustomerOrderRepository.class);
        TrackingEventRepository events = mock(TrackingEventRepository.class);
        AdminTestFeedbackService feedback = mock(AdminTestFeedbackService.class);
        when(services.count()).thenReturn(11L);
        when(quotes.countByStatus(Quote.Status.NEW)).thenReturn(12L);
        when(orders.countByStatus(CustomerOrder.Status.IN_PROGRESS)).thenReturn(13L);
        when(orders.countByStatus(CustomerOrder.Status.COMPLETED)).thenReturn(14L);
        when(events.count()).thenReturn(15L);
        when(feedback.newCount()).thenReturn(16L);
        DashboardService service = new DashboardService(services, quotes, orders, events, feedback);

        ApiDtos.DashboardResponse response = service.get();

        assertEquals(11L, response.totalServices());
        assertEquals(12L, response.newQuotes());
        assertEquals(13L, response.inProgressOrders());
        assertEquals(14L, response.completedOrders());
        assertEquals(15L, response.trackingEvents());
        assertEquals(16L, response.newFeedback());
    }
}
