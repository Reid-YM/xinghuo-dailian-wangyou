package com.xinghuo.catalog.application;

import java.util.List;
import org.springframework.stereotype.Service;

/** Application boundary for public catalog reads, page rendering, and service administration. */
@Service
public class CatalogApplicationService {
    private final CatalogPort port;

    public CatalogApplicationService(CatalogPort port) {
        this.port = port;
    }

    public List<CatalogServiceView> publicList() {
        return port.publicList();
    }

    public List<CatalogPageServiceView> publicPageItems() {
        return port.publicPageItems();
    }

    public CatalogPageServiceView publicPageBySlug(String slug) {
        return port.publicPageBySlug(slug);
    }

    public List<CatalogAdminServiceView> adminList() {
        return port.adminList();
    }

    public CatalogAdminServiceView create(CatalogServiceCommand command, String operator) {
        return port.create(command, operator);
    }

    public CatalogAdminServiceView update(Long id, CatalogServiceCommand command, String operator) {
        return port.update(id, command, operator);
    }

    public void delete(Long id, String operator) {
        port.delete(id, operator);
    }

    public CatalogAdminServiceView restore(Long id, String operator) {
        return port.restore(id, operator);
    }
}
