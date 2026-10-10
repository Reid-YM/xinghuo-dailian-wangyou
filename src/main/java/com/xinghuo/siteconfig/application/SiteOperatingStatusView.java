package com.xinghuo.siteconfig.application;

import com.xinghuo.siteconfig.domain.SiteOperatingStatusCode;

/** Application-owned public operating-status projection. */
public record SiteOperatingStatusView(
        SiteOperatingStatusCode status,
        String shortText,
        String ctaLabel) {

    public String publicCode() {
        return status.publicCode();
    }

    public boolean acceptingOrders() {
        return status.acceptingOrders();
    }
}
