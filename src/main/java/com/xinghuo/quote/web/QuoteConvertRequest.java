package com.xinghuo.quote.web;

import jakarta.validation.constraints.Size;

public record QuoteConvertRequest(
        @Size(max=500) @Size(max=500) String reason) {
}
