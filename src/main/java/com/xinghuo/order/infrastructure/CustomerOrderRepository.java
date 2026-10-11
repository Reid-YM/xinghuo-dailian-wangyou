package com.xinghuo.order.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    Optional<CustomerOrder> findByOrderNo(String orderNo);

    Optional<CustomerOrder> findByBusinessNo(String businessNo);

    Optional<CustomerOrder> findBySourceQuoteId(UUID sourceQuoteId);

    List<CustomerOrder> findTop100ByOrderByUpdatedAtDesc();

    List<CustomerOrder> findTop50ByCustomerAccountIdOrderByUpdatedAtDesc(Long customerAccountId);

    long countByStatus(CustomerOrder.Status status);

    List<CustomerOrder> findByAllowPublicCaseTrueOrderByFinishedAtDescUpdatedAtDesc();
}
