package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LegacyRouteRedirectFilterTest {

    @Test
    void redirectsLegacyOrderIndexToRecords() {
        assertEquals("/records", LegacyRouteRedirectFilter.redirectTarget("/order"));
        assertEquals("/records", LegacyRouteRedirectFilter.redirectTarget("/order/"));
    }

    @Test
    void redirectsLegacyOrderDetailToRecordDetail() {
        assertEquals("/record/XH123", LegacyRouteRedirectFilter.redirectTarget("/order/XH123"));
    }

    @Test
    void redirectsRetiredLoadoutsToMastery() {
        assertEquals("/mastery", LegacyRouteRedirectFilter.redirectTarget("/loadouts"));
        assertEquals("/mastery", LegacyRouteRedirectFilter.redirectTarget("/loadouts/"));
    }

    @Test
    void leavesActiveRoutesUntouched() {
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/records"));
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/record/XH123"));
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/mastery"));
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/api/orders/XH123"));
    }
}
