package com.xinghuo.catalog.application;

import org.springframework.stereotype.Service;

@Service
public class CatalogMetricsApplicationService {
    private final CatalogMetricsPort port;

    public CatalogMetricsApplicationService(CatalogMetricsPort port) {
        this.port = port;
    }

    public long totalServices() {
        return port.totalServices();
    }
}
