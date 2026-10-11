package com.xinghuo.taxonomy.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GameProfileUpdateRequest(
        @NotBlank @Size(max=80) @NotBlank @Size(max=80) String name,
        @Size(max=120) @Size(max=120) String nameEn,
        @Size(max=240) @Size(max=240) String aliases,
        @Size(max=40) @Size(max=40) String iconKey,
        @Size(max=240) @Size(max=240) String description,
        @Size(max=120) @Size(max=120) String publisher,
        @Size(max=160) @Size(max=160) String genres,
        @Size(max=500) @Size(max=500) String logoImage,
        @Size(max=500) @Size(max=500) String coverImage,
        @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer popularityScore,
        Boolean featured,
        @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") String businessStatus,
        @Pattern(regexp="[a-z0-9-]{2,120}") @Pattern(regexp="[a-z0-9-]{2,120}") String seoSlug,
        @Size(max=160) @Size(max=160) String seoTitle,
        @Size(max=300) @Size(max=300) String seoDescription,
        Integer sortOrder,
        Boolean active,
        List<@Size(max=32) String> platformCodes,
        @Size(max=500) @Size(max=500) String reason) {
}
