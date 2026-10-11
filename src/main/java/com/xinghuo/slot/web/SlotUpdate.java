package com.xinghuo.slot.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SlotUpdate(
        @Min(value=1L) @Max(value=2L) @Min(value=1L) @Max(value=2L) int slotNo,
        Long serviceId,
        @NotBlank String mode,
        @Size(max=500) @Size(max=500) String reason) {
}
