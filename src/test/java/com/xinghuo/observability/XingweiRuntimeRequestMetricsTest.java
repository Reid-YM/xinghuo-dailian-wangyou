package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
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
        assertEquals(XingweiRuntimeRequestMetrics.TREND_MINUTES, snapshot.recentMinutes().size());
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

    @Test
    void keepsDisplayTrendAtFifteenMinutesButRetainsSixtyForReleaseObservation() {
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();
        long minute = 1_800_000_000_000L;

        metrics.recordAt(200, minute);
        metrics.recordAt(404, minute + 10_000L);
        metrics.recordAt(503, minute + 60_000L);

        List<XingweiRuntimeRequestMetrics.MinuteBucket> buckets = metrics.recentMinutes(minute + 60_000L);
        XingweiRuntimeRequestMetrics.MinuteBucket previous = buckets.get(buckets.size() - 2);
        XingweiRuntimeRequestMetrics.MinuteBucket current = buckets.get(buckets.size() - 1);

        assertEquals(XingweiRuntimeRequestMetrics.TREND_MINUTES, buckets.size());
        assertEquals(XingweiRuntimeRequestMetrics.RETENTION_MINUTES, metrics.retainedMinutes(minute + 60_000L).size());
        assertEquals(2, previous.totalRequests());
        assertEquals(1, previous.clientErrors());
        assertEquals(0, previous.serverErrors());
        assertEquals(1, current.totalRequests());
        assertEquals(0, current.clientErrors());
        assertEquals(1, current.serverErrors());
    }
}
