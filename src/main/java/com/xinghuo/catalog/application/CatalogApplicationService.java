package com.xinghuo.catalog.application;

import java.util.List;
import org.springframework.stereotype.Service;

/** Application boundary for public catalog reads. */
@Service
public class CatalogApplicationService {
    private final CatalogLegacyPort legacy;

    public CatalogApplicationService(CatalogLegacyPort legacy) {
        this.legacy = legacy;
    }

    public List<CatalogServiceView> publicList() {
        return legacy.publicList();
    }
}
