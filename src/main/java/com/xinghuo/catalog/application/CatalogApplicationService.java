package com.xinghuo.catalog.application;

import com.xinghuo.dto.ApiDtos;
import java.util.List;
import org.springframework.stereotype.Service;

/** Application boundary for public catalog reads. */
@Service
public class CatalogApplicationService {
    private final CatalogLegacyPort legacy;

    public CatalogApplicationService(CatalogLegacyPort legacy) {
        this.legacy = legacy;
    }

    public List<ApiDtos.ServiceResponse> publicList() {
        return legacy.publicList();
    }
}
