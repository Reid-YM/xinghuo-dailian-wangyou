package com.xinghuo.catalog.web;

import com.xinghuo.catalog.application.CatalogAdminServiceView;
import com.xinghuo.catalog.application.CatalogServiceCommand;
import java.util.List;

/** Maps established admin HTTP DTOs to catalog-owned application contracts. */
public final class CatalogAdminWebMapper {
    private CatalogAdminWebMapper() {
    }

    public static CatalogServiceCommand toCommand(ServiceUpsert source) {
        return new CatalogServiceCommand(
                source.gameCode(),
                source.slug(),
                source.name(),
                source.category(),
                source.shortDesc(),
                source.priceMode(),
                source.priceFixed(),
                source.priceMin(),
                source.priceMax(),
                source.durationText(),
                source.status(),
                source.coverImage(),
                source.hotScore(),
                source.manualPriority(),
                source.expiresAt(),
                source.active(),
                source.sortOrder(),
                source.reason());
    }

    public static List<AdminServiceResponse> toResponses(List<CatalogAdminServiceView> views) {
        return views.stream().map(CatalogAdminWebMapper::toResponse).toList();
    }

    public static AdminServiceResponse toResponse(CatalogAdminServiceView view) {
        return new AdminServiceResponse(
                view.id(),
                view.slug(),
                view.gameCode(),
                view.name(),
                view.category(),
                view.shortDesc(),
                view.priceMode(),
                view.priceFixed(),
                view.priceMin(),
                view.priceMax(),
                view.durationText(),
                view.status(),
                view.coverImage(),
                view.hotScore(),
                view.manualPriority(),
                view.expiresAt(),
                view.active(),
                view.sortOrder(),
                view.version());
    }
}
