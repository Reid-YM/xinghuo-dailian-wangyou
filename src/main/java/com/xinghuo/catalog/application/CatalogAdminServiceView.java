package com.xinghuo.catalog.application;

import java.math.BigDecimal;
import java.time.Instant;

/** Framework-neutral view returned by catalog administration use cases. */
public record CatalogAdminServiceView(
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
        String coverImage,
        BigDecimal hotScore,
        int manualPriority,
        Instant expiresAt,
        boolean active,
        int sortOrder,
        long version) {
}
