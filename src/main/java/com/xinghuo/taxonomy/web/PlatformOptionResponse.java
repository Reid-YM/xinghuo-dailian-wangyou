package com.xinghuo.taxonomy.web;

public record PlatformOptionResponse(
        String code,
        String name,
        String iconKey,
        String platformGroup,
        String description,
        boolean active,
        int sortOrder) {
}
