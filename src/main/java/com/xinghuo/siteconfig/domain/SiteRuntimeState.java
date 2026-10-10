package com.xinghuo.siteconfig.domain;

/** Runtime switches and fallback operating state used by public pages and APIs. */
public record SiteRuntimeState(
        boolean quoteEnabled,
        boolean orderQueryEnabled,
        SiteOperatingStatusCode operatingStatus,
        String statusShortText,
        String primaryButtonLabel) {
}
