package com.xinghuo.catalog.web;

import com.xinghuo.catalog.application.CatalogServiceView;
import com.xinghuo.dto.ApiDtos;
import java.util.List;

/** Maps catalog application views to the established public API DTO. */
public final class CatalogWebMapper {
    private CatalogWebMapper() {
    }

    public static List<ApiDtos.ServiceResponse> toResponses(List<CatalogServiceView> views) {
        return views.stream().map(CatalogWebMapper::toResponse).toList();
    }

    private static ApiDtos.ServiceResponse toResponse(CatalogServiceView view) {
        return new ApiDtos.ServiceResponse(
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
                view.coverImage());
    }
}
