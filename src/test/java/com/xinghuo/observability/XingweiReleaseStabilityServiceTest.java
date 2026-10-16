package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class XingweiReleaseStabilityServiceTest {
    @Test
    void completesFiveMinuteWindowAndRaisesWarningForThreeServerErrors() {
        Instant now = Instant.parse("2026-10-17T06:10:00Z");
        Instant publishedAt = now.minusSeconds(6 * 60L);
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();
        metrics.recordAt(200, publishedAt.plusSeconds(60).toEpochMilli());
        metrics.recordAt(500, publishedAt.plusSeconds(90).toEpochMilli());
        metrics.recordAt(502, publishedAt.plusSeconds(120).toEpochMilli());
        metrics.recordAt(503, publishedAt.plusSeconds(180).toEpochMilli());

        XingweiReleaseStabilityService service = service(now, publishedAt, now.minusSeconds(10 * 60L), metrics);
        XingweiReleaseStabilityService.Snapshot snapshot = service.snapshot();

        assertTrue(snapshot.current().available());
        assertEquals("COVERED", snapshot.current().coverageState());
        assertEquals("WARNING", snapshot.current().severity());
        XingweiReleaseStabilityService.WindowObservation five = snapshot.current().windows().get(0);
        assertEquals(5, five.minutes());
        assertEquals("COMPLETE", five.state());
        assertEquals(4, five.totalRequests());
        assertEquals(3, five.serverErrors());
        assertEquals("WARNING", five.severity());
        assertEquals("IN_PROGRESS", snapshot.current().windows().get(1).state());
        assertEquals("IN_PROGRESS", snapshot.current().windows().get(2).state());
    }

    @Test
    void refusesToReconstructWindowAfterJvmRestart() {
        Instant now = Instant.parse("2026-10-17T06:10:00Z");
        Instant publishedAt = now.minusSeconds(8 * 60L);
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();
        metrics.recordAt(200, now.minusSeconds(60).toEpochMilli());

        XingweiReleaseStabilityService service = service(now, publishedAt, publishedAt.plusSeconds(120), metrics);
        XingweiReleaseStabilityService.Snapshot snapshot = service.snapshot();

        assertEquals("PROCESS_RESTARTED", snapshot.current().coverageState());
        assertEquals("UNKNOWN", snapshot.current().severity());
        assertTrue(snapshot.current().windows().stream()
                .allMatch(window -> "PROCESS_RESTARTED".equals(window.state())));
    }

    @Test
    void marksObservationExpiredWhenPublishedMinuteFallsOutsideRetention() {
        Instant now = Instant.parse("2026-10-17T08:00:00Z");
        Instant publishedAt = now.minusSeconds(90 * 60L);
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();
        metrics.recordAt(200, now.minusSeconds(60).toEpochMilli());

        XingweiReleaseStabilityService service = service(now, publishedAt, publishedAt.minusSeconds(60), metrics);
        XingweiReleaseStabilityService.Snapshot snapshot = service.snapshot();

        assertEquals("EXPIRED", snapshot.current().coverageState());
        assertEquals(XingweiRuntimeRequestMetrics.RETENTION_MINUTES, snapshot.processRetentionMinutes());
        assertTrue(snapshot.current().windows().stream()
                .allMatch(window -> "EXPIRED".equals(window.state())));
    }

    @Test
    void keepsHistoryBaselineIndependentFromCurrentObservationCoverage() {
        Instant now = Instant.parse("2026-10-17T06:10:00Z");
        Instant publishedAt = now.minusSeconds(8 * 60L);
        XingweiRuntimeRequestMetrics metrics = new XingweiRuntimeRequestMetrics();

        XingweiRuntimeHealthService runtime = mock(XingweiRuntimeHealthService.class);
        when(runtime.snapshot()).thenReturn(runtimeSnapshot(now, publishedAt.plusSeconds(60), metrics));
        XingweiReleaseAttestationService attestation = mock(XingweiReleaseAttestationService.class);
        when(attestation.snapshot()).thenReturn(attestationSnapshot(now, publishedAt));
        XingweiReleaseHistoryService history = mock(XingweiReleaseHistoryService.class);
        XingweiReleaseHistoryService.Baseline baseline = new XingweiReleaseHistoryService.Baseline(
                true, "ESTABLISHED", 2, 55, 2, 0, 52, -3);
        when(history.snapshot()).thenReturn(new XingweiReleaseHistoryService.Snapshot(
                true,
                "AVAILABLE",
                now,
                List.of(),
                baseline));

        XingweiReleaseStabilityService.Snapshot snapshot = new XingweiReleaseStabilityService(
                runtime,
                metrics,
                attestation,
                history).snapshot();

        assertEquals("PROCESS_RESTARTED", snapshot.current().coverageState());
        assertTrue(snapshot.baseline().ready());
        assertEquals(55, snapshot.baseline().medianPostDeployMaxMillis());
        assertFalse(snapshot.history().size() > 10);
    }

    private XingweiReleaseStabilityService service(
            Instant now,
            Instant publishedAt,
            Instant runtimeStartedAt,
            XingweiRuntimeRequestMetrics metrics) {
        XingweiRuntimeHealthService runtime = mock(XingweiRuntimeHealthService.class);
        when(runtime.snapshot()).thenReturn(runtimeSnapshot(now, runtimeStartedAt, metrics));
        XingweiReleaseAttestationService attestation = mock(XingweiReleaseAttestationService.class);
        when(attestation.snapshot()).thenReturn(attestationSnapshot(now, publishedAt));
        XingweiReleaseHistoryService history = mock(XingweiReleaseHistoryService.class);
        when(history.snapshot()).thenReturn(new XingweiReleaseHistoryService.Snapshot(
                true,
                "EMPTY",
                now,
                List.of(),
                XingweiReleaseHistoryService.Baseline.empty()));
        return new XingweiReleaseStabilityService(runtime, metrics, attestation, history);
    }

    private XingweiRuntimeHealthService.Snapshot runtimeSnapshot(
            Instant now,
            Instant startedAt,
            XingweiRuntimeRequestMetrics metrics) {
        return new XingweiRuntimeHealthService.Snapshot(
                now,
                startedAt,
                Math.max(0L, now.toEpochMilli() - startedAt.toEpochMilli()),
                new XingweiRuntimeHealthService.DatabaseSnapshot(true, 5),
                new XingweiRuntimeHealthService.JvmSnapshot(10, 100, 10),
                metrics.snapshot());
    }

    private XingweiReleaseAttestationService.Snapshot attestationSnapshot(Instant now, Instant publishedAt) {
        XingweiReleaseAttestationService.HealthComparison comparison = new XingweiReleaseAttestationService.HealthComparison(
                new XingweiReleaseAttestationService.HealthProbe(true, 40, 45),
                new XingweiReleaseAttestationService.HealthProbe(true, 42, 50));
        XingweiReleaseAttestationService.Attestation item = new XingweiReleaseAttestationService.Attestation(
                2,
                publishedAt,
                "a".repeat(40),
                "b".repeat(64),
                "xinghuo-v2-standard-20261017060000.jar",
                "xinghuo-v2-standard-20261017050000.jar",
                8080,
                8081,
                8080,
                true,
                new XingweiReleaseAttestationService.Checks(true, true, true),
                comparison);
        return new XingweiReleaseAttestationService.Snapshot(true, "AVAILABLE", now, item);
    }
}
