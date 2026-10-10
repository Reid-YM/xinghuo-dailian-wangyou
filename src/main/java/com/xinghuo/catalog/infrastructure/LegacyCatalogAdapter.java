package com.xinghuo.catalog.infrastructure;

import com.xinghuo.catalog.application.CatalogLegacyPort;
import com.xinghuo.catalog.application.CatalogServiceView;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.ServiceCatalogService;
import java.util.List;
import org.springframework.stereotype.Component;

/** Bridges application-owned catalog views to the existing catalog service. */
@Component
public class LegacyCatalogAdapter implements CatalogLegacyPort {
    private final ServiceCatalogService legacy;

    public LegacyCatalogAdapter(ServiceCatalogService legacy) {
        this.legacy = legacy;
    }

    @Override
    public List<CatalogServiceView> publicList() {
        return legacy.publicList().stream().map(this::view).toList();
    }

    private CatalogServiceView view(ApiDtos.ServiceResponse source) {
        return new CatalogServiceView(
                source.id(),
                source.slug(),
                source.gameCode(),
                source.name(),
                source.category(),
                source.shortDesc(),
                source.priceMode(),
                source.priceFixed(),
                source.priceMin(),
                source.priceMax(),
                source.durationText(),
                source.status(),
                source.coverImage());
    }
}
