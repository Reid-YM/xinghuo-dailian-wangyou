package com.xinghuo.order.infrastructure;

import com.xinghuo.model.CustomerOrder;
import com.xinghuo.order.application.OrderMetricsPort;
import com.xinghuo.repository.CustomerOrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderMetricsJpaAdapter implements OrderMetricsPort {
    private final CustomerOrderRepository repo;

    public OrderMetricsJpaAdapter(CustomerOrderRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public long countInProgress() {
        return repo.countByStatus(CustomerOrder.Status.IN_PROGRESS);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCompleted() {
        return repo.countByStatus(CustomerOrder.Status.COMPLETED);
    }
}
