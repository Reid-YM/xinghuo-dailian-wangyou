package com.xinghuo.siteconfig.domain;

/** Runtime switches that gate public quote and order-query flows. */
public record SiteRuntimeState(boolean quoteEnabled, boolean orderQueryEnabled) {
}
