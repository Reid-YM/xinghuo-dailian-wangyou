package com.xinghuo.taxonomy.web;

import java.util.List;

public record GameOptionResponse(
        String code,
        String name,
        String nameEn,
        String aliases,
        String iconKey,
        String description,
        String publisher,
        String genres,
        String logoImage,
        String coverImage,
        int popularityScore,
        boolean featured,
        String businessStatus,
        String seoSlug,
        String seoTitle,
        String seoDescription,
        boolean active,
        int sortOrder,
        long categoryCount,
        long serviceCount,
        List<String> platformCodes) {
}
