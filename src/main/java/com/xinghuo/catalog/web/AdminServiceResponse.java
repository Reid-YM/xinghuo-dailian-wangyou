package com.xinghuo.catalog.web;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminServiceResponse(
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
