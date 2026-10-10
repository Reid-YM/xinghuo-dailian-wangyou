package com.xinghuo.catalog.application;

import java.math.BigDecimal;
import java.time.Instant;

/** Framework-neutral command for catalog service administration. */
public record CatalogServiceCommand(
        String gameCode,
        String slug,
        String name,
        String category,
        String shortDesc,
        String priceMode,
        BigDecimal priceFixed,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String durationText,
        String status,
        String coverImage,
        BigDecimal hotScore,
        Integer manualPriority,
        Instant expiresAt,
        Boolean active,
        Integer sortOrder,
        String reason) {
}
