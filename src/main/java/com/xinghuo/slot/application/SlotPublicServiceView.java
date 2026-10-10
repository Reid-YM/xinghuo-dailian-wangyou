package com.xinghuo.slot.application;

import java.math.BigDecimal;

/** Public service projection selected by the home-page slot policy. */
public record SlotPublicServiceView(
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
