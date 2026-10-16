package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class XingweiRuntimeRequestMetricsTest {
    @Test
    void countsRequestsByStatusFamilyWithoutBusinessContext() {
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();

        metrics.record(200);
        metrics.record(302);
        metrics.record(404);

        XingweiRuntimeRequestMetrics.Snapshot snapshot = metrics.snapshot();
        assertEquals(3, snapshot.totalRequests());
        assertEquals(1, snapshot.clientErrors());
        assertEquals(0, snapshot.serverErrors());
        assertNull(snapshot.lastServerError());
    }

    @Test
    void remembersOnlyLatestServerErrorTimeAndStatus() {
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();

        metrics.record(500);
        metrics.record(503);

        XingweiRuntimeRequestMetrics.Snapshot snapshot = metrics.snapshot();
        assertEquals(2, snapshot.totalRequests());
        assertEquals(0, snapshot.clientErrors());
        assertEquals(2, snapshot.serverErrors());
        assertNotNull(snapshot.lastServerError());
        assertEquals(503, snapshot.lastServerError().status());
        assertNotNull(snapshot.lastServerError().at());
    }
}
