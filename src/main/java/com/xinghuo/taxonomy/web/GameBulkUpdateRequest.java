package com.xinghuo.taxonomy.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GameBulkUpdateRequest(
        @NotEmpty @Size(max=100) @NotEmpty @Size(max=100) List<@Size(max=32) String> codes,
        Boolean active,
        Boolean featured,
        @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") @Pattern(regexp="ACTIVE|READY|PLANNED|PAUSED") String businessStatus,
        @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer popularityScore,
        @Size(max=500) @Size(max=500) String reason) {
}
