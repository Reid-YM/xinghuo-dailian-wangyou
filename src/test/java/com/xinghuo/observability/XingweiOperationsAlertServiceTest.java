package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class XingweiOperationsAlertServiceTest {
    private final XingweiRuntimeHealthService runtimeHealthService = mock(XingweiRuntimeHealthService.class);
    private final XingweiReleaseAttestationService releaseAttestationService = mock(XingweiReleaseAttestationService.class);
    private final XingweiOperationsAlertService service =
            new XingweiOperationsAlertService(runtimeHealthService, releaseAttestationService);

    @Test
    void healthyRuntimeAndStableV2ReleaseProducesOkSummary() {
        when(runtimeHealthService.snapshot()).thenReturn(runtime(true, 12, 45, 1_000, 0));
        when(releaseAttestationService.snapshot()).thenReturn(releaseV2(40, 55, 45, 61));

        XingweiOperationsAlertService.Snapshot snapshot = service.snapshot();

        assertEquals("OK", snapshot.severity());
        assertEquals(0, snapshot.activeAlertCount());
        assertEquals("STABLE", snapshot.releaseComparison().state());
        assertTrue(snapshot.releaseComparison().available());
    }

    @Test
    void databaseFailureIsCriticalAndIncludesSafeRecommendedAction() {
        when(runtimeHealthService.snapshot()).thenReturn(runtime(false, 5, 35, 20, 0));
        when(releaseAttestationService.snapshot()).thenReturn(releaseV2(40, 55, 45, 61));

        XingweiOperationsAlertService.Snapshot snapshot = service.snapshot();

        assertEquals("CRITICAL", snapshot.severity());
        assertTrue(snapshot.alerts().stream().anyMatch(alert -> alert.code().equals("database-down")));
        assertTrue(snapshot.alerts().stream().allMatch(alert -> !alert.detail().contains("jdbc:")));
    }

    @Test
    void repeatedRecentServerErrorsReachWarningThreshold() {
        when(runtimeHealthService.snapshot()).thenReturn(runtime(true, 8, 40, 120, 3));
        when(releaseAttestationService.snapshot()).thenReturn(releaseV2(40, 55, 45, 61));

        XingweiOperationsAlertService.Snapshot snapshot = service.snapshot();

        assertEquals("WARNING", snapshot.severity());
        assertEquals(3, snapshot.runtime().recentServerErrors());
        assertTrue(snapshot.alerts().stream().anyMatch(alert -> alert.code().equals("server-errors-warning")));
    }

    @Test
    void v1ReleaseCredentialRemainsReadableButEntersWatchUntilNextDeploy() {
        when(runtimeHealthService.snapshot()).thenReturn(runtime(true, 8, 40, 120, 0));
        when(releaseAttestationService.snapshot()).thenReturn(releaseV1());

        XingweiOperationsAlertService.Snapshot snapshot = service.snapshot();

        assertEquals("WATCH", snapshot.severity());
        assertEquals("PENDING", snapshot.releaseComparison().state());
        assertTrue(snapshot.alerts().stream().anyMatch(alert -> alert.code().equals("release-health-comparison-pending")));
    }

    @Test
    void materiallySlowerPostDeployProbeCreatesWatchWithoutCallingItOutage() {
        when(runtimeHealthService.snapshot()).thenReturn(runtime(true, 8, 40, 120, 0));
        when(releaseAttestationService.snapshot()).thenReturn(releaseV2(80, 120, 700, 850));

        XingweiOperationsAlertService.Snapshot snapshot = service.snapshot();

        assertEquals("WATCH", snapshot.severity());
        assertEquals("SLOWER", snapshot.releaseComparison().state());
        assertTrue(snapshot.alerts().stream().anyMatch(alert -> alert.code().equals("release-latency-watch")));
    }

    private XingweiRuntimeHealthService.Snapshot runtime(
            boolean databaseUp,
            long databaseMillis,
            double heapPercent,
            long recentRequests,
            long recentServerErrors) {
        long maxHeap = 1_000_000L;
        long usedHeap = Math.round(maxHeap * (heapPercent / 100.0));
        List<XingweiRuntimeRequestMetrics.MinuteBucket> buckets = List.of(
                bucket(0, recentRequests / 5, recentServerErrors > 0 ? 1 : 0),
                bucket(1, recentRequests / 5, recentServerErrors > 1 ? 1 : 0),
                bucket(2, recentRequests / 5, recentServerErrors > 2 ? 1 : 0),
                bucket(3, recentRequests / 5, recentServerErrors > 3 ? 1 : 0),
                bucket(4, recentRequests - (recentRequests / 5) * 4, Math.max(0, recentServerErrors - 4)));
        long totalErrors = buckets.stream().mapToLong(XingweiRuntimeRequestMetrics.MinuteBucket::serverErrors).sum();
        long totalRequests = buckets.stream().mapToLong(XingweiRuntimeRequestMetrics.MinuteBucket::totalRequests).sum();
        return new XingweiRuntimeHealthService.Snapshot(
                Instant.parse("2026-10-17T05:20:00Z"),
                Instant.parse("2026-10-17T05:00:00Z"),
                1_200_000L,
                new XingweiRuntimeHealthService.DatabaseSnapshot(databaseUp, databaseMillis),
                new XingweiRuntimeHealthService.JvmSnapshot(usedHeap, maxHeap, 48),
                new XingweiRuntimeRequestMetrics.Snapshot(totalRequests, 0, totalErrors, null, buckets));
    }

    private XingweiRuntimeRequestMetrics.MinuteBucket bucket(int offset, long requests, long serverErrors) {
        return new XingweiRuntimeRequestMetrics.MinuteBucket(
                Instant.parse("2026-10-17T05:0" + offset + ":00Z"),
                requests,
                0,
                serverErrors);
    }

    private XingweiReleaseAttestationService.Snapshot releaseV1() {
        return new XingweiReleaseAttestationService.Snapshot(
                true,
                "AVAILABLE",
                Instant.parse("2026-10-17T05:20:00Z"),
                attestation(1, null));
    }

    private XingweiReleaseAttestationService.Snapshot releaseV2(
            int beforeHome,
            int beforeServices,
            int afterHome,
            int afterServices) {
        XingweiReleaseAttestationService.HealthComparison comparison =
                new XingweiReleaseAttestationService.HealthComparison(
                        new XingweiReleaseAttestationService.HealthProbe(true, beforeHome, beforeServices),
                        new XingweiReleaseAttestationService.HealthProbe(true, afterHome, afterServices));
        return new XingweiReleaseAttestationService.Snapshot(
                true,
                "AVAILABLE",
                Instant.parse("2026-10-17T05:20:00Z"),
                attestation(2, comparison));
    }

    private XingweiReleaseAttestationService.Attestation attestation(
            int schemaVersion,
            XingweiReleaseAttestationService.HealthComparison comparison) {
        return new XingweiReleaseAttestationService.Attestation(
                schemaVersion,
                Instant.parse("2026-10-17T05:10:00Z"),
                "acf8af76b156047ff33072cbeaf6b7789b8bb624",
                "d797326b0ddbcc1601db98ad59cda36d4fd709ad01d763458b107a874dbaf4ba",
                "xinghuo-v2-standard-20261017050537.jar",
                "xinghuo-v2-standard-20261017040039.jar",
                8080,
                8081,
                8080,
                true,
                new XingweiReleaseAttestationService.Checks(true, true, true),
                comparison);
    }
}
