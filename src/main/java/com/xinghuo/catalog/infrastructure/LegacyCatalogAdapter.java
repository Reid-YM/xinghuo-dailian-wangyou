package com.xinghuo.catalog.infrastructure;

import com.xinghuo.catalog.application.CatalogLegacyPort;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.ServiceCatalogService;
import java.util.List;
import org.springframework.stereotype.Component;

/** Bridges the new catalog boundary to the existing catalog service. */
@Component
public class LegacyCatalogAdapter implements CatalogLegacyPort {
    private final ServiceCatalogService legacy;

    public LegacyCatalogAdapter(ServiceCatalogService legacy) {
        this.legacy = legacy;
    }

    @Override
    public List<ApiDtos.ServiceResponse> publicList() {
        return legacy.publicList();
    }
}
