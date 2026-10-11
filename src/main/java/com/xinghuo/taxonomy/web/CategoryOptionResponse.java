package com.xinghuo.taxonomy.web;

public record CategoryOptionResponse(
        Long id,
        String gameCode,
        String code,
        String name,
        String iconKey,
        String description,
        boolean active,
        int sortOrder,
        long serviceCount) {
}
