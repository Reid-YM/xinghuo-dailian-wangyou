package com.xinghuo.catalog.application;

import java.math.BigDecimal;

/**
 * Typed, framework-neutral service projection for Thymeleaf page rendering and SEO.
 * JavaBean getters intentionally preserve the existing template property contract.
 */
public record CatalogPageServiceView(
        Long id,
        String slug,
        String gameCode,
        String name,
        String category,
        String shortDesc,
        PriceMode priceMode,
        BigDecimal priceFixed,
        BigDecimal priceMin,
        BigDecimal priceMax,
        String durationText,
        Status status,
        String coverImage) {

    public enum PriceMode {
        FIXED,
        RANGE,
        QUOTE
    }

    public enum Status {
        AVAILABLE,
        BUSY,
        OFFLINE
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getGameCode() {
        return gameCode;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public PriceMode getPriceMode() {
        return priceMode;
    }

    public BigDecimal getPriceFixed() {
        return priceFixed;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public String getDurationText() {
        return durationText;
    }

    public Status getStatus() {
        return status;
    }

    public String getCoverImage() {
        return coverImage;
    }
}
