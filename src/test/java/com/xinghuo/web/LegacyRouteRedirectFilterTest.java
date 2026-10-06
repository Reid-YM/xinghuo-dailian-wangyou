package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void retiresLoadoutApi() {
        assertTrue(LegacyRouteRedirectFilter.isRetiredApi("/api/loadouts"));
        assertTrue(LegacyRouteRedirectFilter.isRetiredApi("/api/loadouts/abc"));
        assertFalse(LegacyRouteRedirectFilter.isRetiredApi("/api/tool-station"));
    }

    @Test
    void leavesActiveRoutesUntouched() {
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/records"));
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/record/XH123"));
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/mastery"));
        assertNull(LegacyRouteRedirectFilter.redirectTarget("/api/orders/XH123"));
    }
}
