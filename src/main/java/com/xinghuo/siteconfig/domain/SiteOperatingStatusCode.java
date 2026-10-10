package com.xinghuo.siteconfig.domain;

import java.util.Locale;

/** Site-level public operating status independent of persistence entities. */
public enum SiteOperatingStatusCode {
    OPEN,
    SUSPENDED,
    MAINTENANCE,
    RESERVATION;

    public boolean acceptingOrders() {
        return this == OPEN;
    }

    public String publicCode() {
        return name().toLowerCase(Locale.ROOT);
    }
}
