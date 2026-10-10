package com.xinghuo.catalog.application;

import java.math.BigDecimal;

/** Application-owned read model for one public catalog service. */
public record CatalogServiceView(
        Long id,
        String slug,
        String gameCode,
        String name,
        String category,
        String shortDesc,
        String priceMode,
        BigDecimal priceFixed,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String durationText,
        String status,
        String coverImage) {
}
