package com.xinghuo.catalog.application;

import java.util.List;

/** Catalog-owned persistence and integration boundary. */
public interface CatalogPort {
    List<CatalogServiceView> publicList();

    List<CatalogPageServiceView> publicPageItems();

    CatalogPageServiceView publicPageBySlug(String slug);

    List<CatalogAdminServiceView> adminList();

    CatalogAdminServiceView create(CatalogServiceCommand command, String operator);

    CatalogAdminServiceView update(Long id, CatalogServiceCommand command, String operator);

    void delete(Long id, String operator);

    CatalogAdminServiceView restore(Long id, String operator);
}
