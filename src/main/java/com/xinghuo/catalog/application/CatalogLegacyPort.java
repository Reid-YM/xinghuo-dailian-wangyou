package com.xinghuo.catalog.application;

import java.util.List;

/** Temporary migration port while catalog behavior is moved out of the legacy service. */
public interface CatalogLegacyPort {
    List<CatalogServiceView> publicList();

    List<CatalogPageServiceView> publicPageItems();

    CatalogPageServiceView publicPageBySlug(String slug);

    List<CatalogAdminServiceView> adminList();

    CatalogAdminServiceView create(CatalogServiceCommand command, String operator);

    CatalogAdminServiceView update(Long id, CatalogServiceCommand command, String operator);

    void delete(Long id, String operator);

    CatalogAdminServiceView restore(Long id, String operator);
}
