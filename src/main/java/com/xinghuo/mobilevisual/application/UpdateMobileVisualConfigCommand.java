package com.xinghuo.mobilevisual.application;

/** Typed application command for mobile visual tuning. */
public record UpdateMobileVisualConfigCommand(
        int heroBackgroundMobileOpacity,
        int heroBackgroundMobileBlur) {
}
