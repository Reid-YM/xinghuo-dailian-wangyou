package com.xinghuo.quote.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuoteStatusUpdate(
        @NotBlank String status,
        @Size(max=500) @Size(max=500) String reason) {
}
