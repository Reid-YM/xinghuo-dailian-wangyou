/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.CustomerOrder
 *  com.xinghuo.model.CustomerOrder$Status
 *  com.xinghuo.repository.CustomerOrderRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.xinghuo.repository;

import com.xinghuo.model.CustomerOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository
extends JpaRepository<CustomerOrder, UUID> {
    public Optional<CustomerOrder> findByOrderNo(String var1);

    public Optional<CustomerOrder> findByBusinessNo(String var1);

    public Optional<CustomerOrder> findBySourceQuoteId(UUID var1);

    public List<CustomerOrder> findTop100ByOrderByUpdatedAtDesc();

    public List<CustomerOrder> findTop50ByCustomerAccountIdOrderByUpdatedAtDesc(Long var1);

    public long countByStatus(CustomerOrder.Status var1);

    public List<CustomerOrder> findByAllowPublicCaseTrueOrderByFinishedAtDescUpdatedAtDesc();
}

