package com.xinghuo.order.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotBlank String status,
        @Min(value=0L) @Max(value=100L) @Min(value=0L) @Max(value=100L) Integer progressPercent,
        @NotBlank @Size(max=240) @NotBlank @Size(max=240) String progressText,
        @Size(max=500) @Size(max=500) String customerNote,
        @Size(max=1200) @Size(max=1200) String internalNote,
        @Size(max=500) @Size(max=500) String reason) {
}
