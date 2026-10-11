package com.xinghuo.tracking.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrackRequest(
        @NotBlank @Size(max=64) @NotBlank @Size(max=64) String eventName,
        @Size(max=255) @Size(max=255) String path,
        @Size(max=255) @Size(max=255) String refValue) {
}
