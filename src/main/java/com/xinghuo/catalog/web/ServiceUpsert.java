package com.xinghuo.catalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record ServiceUpsert(
        @NotBlank @Size(max=32) @NotBlank @Size(max=32) String gameCode,
        @NotBlank @Pattern(regexp="[a-z0-9-]{2,100}") @NotBlank @Pattern(regexp="[a-z0-9-]{2,100}") String slug,
        @NotBlank @Size(max=120) @NotBlank @Size(max=120) String name,
        @NotBlank String category,
        @NotBlank @Size(max=240) @NotBlank @Size(max=240) String shortDesc,
        @NotBlank String priceMode,
        BigDecimal priceFixed,
        BigDecimal priceMin,
        BigDecimal priceMax,
        @NotBlank @Size(max=100) @NotBlank @Size(max=100) String durationText,
        @NotBlank String status,
        @Size(max=500) @Size(max=500) String coverImage,
        BigDecimal hotScore,
        Integer manualPriority,
        Instant expiresAt,
        Boolean active,
        Integer sortOrder,
        @Size(max=500) @Size(max=500) String reason) {
}
