package com.xinghuo.catalog.application;

import java.util.List;
import org.springframework.stereotype.Service;

/** Application boundary for public catalog reads and service administration. */
@Service
public class CatalogApplicationService {
    private final CatalogLegacyPort legacy;

    public CatalogApplicationService(CatalogLegacyPort legacy) {
        this.legacy = legacy;
    }

    public List<CatalogServiceView> publicList() {
        return legacy.publicList();
    }

    public List<CatalogAdminServiceView> adminList() {
        return legacy.adminList();
    }

    public CatalogAdminServiceView create(CatalogServiceCommand command, String operator) {
        return legacy.create(command, operator);
    }

    public CatalogAdminServiceView update(Long id, CatalogServiceCommand command, String operator) {
        return legacy.update(id, command, operator);
    }

    public void delete(Long id, String operator) {
        legacy.delete(id, operator);
    }

    public CatalogAdminServiceView restore(Long id, String operator) {
        return legacy.restore(id, operator);
    }
}
