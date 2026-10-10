package com.xinghuo.catalog.infrastructure;

import com.xinghuo.catalog.application.CatalogMetricsPort;
import com.xinghuo.repository.ServiceItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogMetricsJpaAdapter implements CatalogMetricsPort {
    private final ServiceItemRepository repo;

    public CatalogMetricsJpaAdapter(ServiceItemRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public long totalServices() {
        return repo.count();
    }
}
