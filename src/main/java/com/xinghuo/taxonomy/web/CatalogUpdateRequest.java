package com.xinghuo.taxonomy.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogUpdateRequest(
        @NotBlank @Size(max=80) @NotBlank @Size(max=80) String name,
        @Size(max=40) @Size(max=40) String iconKey,
        @Size(max=240) @Size(max=240) String description,
        Integer sortOrder,
        Boolean active,
        @Size(max=500) @Size(max=500) String reason) {
}
