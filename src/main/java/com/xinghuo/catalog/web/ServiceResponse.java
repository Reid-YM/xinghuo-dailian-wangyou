package com.xinghuo.catalog.web;

import java.math.BigDecimal;

public record ServiceResponse(
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
