package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class XingweiRuntimeHealthServiceTest {
    @Test
    void reportsDatabaseUpAndRuntimeSnapshot() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();
        metrics.record(200);

        XingweiRuntimeHealthService.Snapshot snapshot =
                new XingweiRuntimeHealthService(jdbc, metrics).snapshot();

        assertTrue(snapshot.database().up());
        assertTrue(snapshot.database().checkMillis() >= 0);
        assertTrue(snapshot.uptimeMillis() >= 0);
        assertTrue(snapshot.jvm().usedHeapBytes() >= 0);
        assertTrue(snapshot.jvm().maxHeapBytes() > 0);
        assertTrue(snapshot.jvm().threadCount() > 0);
        assertNotNull(snapshot.startedAt());
        assertNotNull(snapshot.sampledAt());
        assertTrue(snapshot.requests().totalRequests() >= 1);
    }

    @Test
    void databaseFailureIsReportedWithoutLeakingException() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new IllegalStateException("jdbc://secret-host/example"));

        XingweiRuntimeHealthService.Snapshot snapshot =
                new XingweiRuntimeHealthService(jdbc, new XingweiRuntimeRequestMetrics()).snapshot();

        assertFalse(snapshot.database().up());
        assertTrue(snapshot.database().checkMillis() >= 0);
    }
}
