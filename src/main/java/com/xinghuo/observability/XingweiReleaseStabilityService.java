package com.xinghuo.observability;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 组合当前 JVM 分钟级聚合指标与生产主机发布历史，形成发布后观察窗口和历史稳定性基线。
 *
 * <p>应用不会写入主机发布历史，也不会据此执行发布、回滚或重启。5/15/30 分钟窗口仅使用
 * 聚合请求数量、4xx、5xx；JVM 重启或分钟保留窗口过期后会明确标记覆盖不足，不补造历史数据。
 */
@Service
public class XingweiReleaseStabilityService {
    private static final int[] OBSERVATION_WINDOWS = {5, 15, 30};
    private static final long RATE_MIN_REQUESTS = 50L;
    private static final double WARNING_RATE = 0.03;
    private static final double CRITICAL_RATE = 0.10;
    private static final long WARNING_COUNT = 3L;
    private static final long CRITICAL_COUNT = 10L;
    private static final int HISTORY_DISPLAY_LIMIT = 10;

    private final XingweiRuntimeHealthService runtimeHealthService;
    private final XingweiRuntimeRequestMetrics requestMetrics;
    private final XingweiReleaseAttestationService attestationService;
    private final XingweiReleaseHistoryService historyService;

    public XingweiReleaseStabilityService(
            XingweiRuntimeHealthService runtimeHealthService,
            XingweiRuntimeRequestMetrics requestMetrics,
            XingweiReleaseAttestationService attestationService,
            XingweiReleaseHistoryService historyService) {
        this.runtimeHealthService = runtimeHealthService;
        this.requestMetrics = requestMetrics;
        this.attestationService = attestationService;
        this.historyService = historyService;
    }

    public Snapshot snapshot() {
        XingweiRuntimeHealthService.Snapshot runtime = runtimeHealthService.snapshot();
        XingweiReleaseAttestationService.Snapshot release = attestationService.snapshot();
        XingweiReleaseHistoryService.Snapshot history = historyService.snapshot();

        CurrentObservation current = observeCurrentRelease(runtime, release);
        List<XingweiReleaseHistoryService.Entry> historyEntries = history.entries().stream()
                .limit(HISTORY_DISPLAY_LIMIT)
                .toList();
        return new Snapshot(
                runtime.sampledAt(),
                current,
                history.available(),
                history.status(),
                history.baseline(),
                historyEntries,
                60,
                XingweiRuntimeRequestMetrics.RETENTION_MINUTES);
    }

    private CurrentObservation observeCurrentRelease(
            XingweiRuntimeHealthService.Snapshot runtime,
            XingweiReleaseAttestationService.Snapshot release) {
        if (!release.available() || release.attestation() == null) {
            return CurrentObservation.unavailable(release.status(), runtime.startedAt());
        }

        Instant now = runtime.sampledAt();
        Instant publishedAt = release.attestation().publishedAt();
        long ageSeconds = Math.max(0L, Duration.between(publishedAt, now).getSeconds());
        List<XingweiRuntimeRequestMetrics.MinuteBucket> retained =
                requestMetrics.retainedMinutes(now.toEpochMilli());
        Instant publishedMinute = publishedAt.truncatedTo(ChronoUnit.MINUTES);
        Instant earliestRetained = retained.isEmpty() ? now : retained.get(0).minute();

        String coverageState;
        if (now.isBefore(publishedAt)) {
            coverageState = "CLOCK_SKEW";
        } else if (runtime.startedAt().isAfter(publishedAt)) {
            coverageState = "PROCESS_RESTARTED";
        } else if (earliestRetained.isAfter(publishedMinute)) {
            coverageState = "EXPIRED";
        } else {
            coverageState = "COVERED";
        }

        List<WindowObservation> windows = new ArrayList<>(OBSERVATION_WINDOWS.length);
        for (int minutes : OBSERVATION_WINDOWS) {
            windows.add(observeWindow(retained, publishedAt, now, minutes, coverageState));
        }
        String severity = windows.stream()
                .map(WindowObservation::severity)
                .max(Comparator.comparingInt(this::severityRank))
                .orElse("OK");

        return new CurrentObservation(
                true,
                "AVAILABLE",
                publishedAt,
                runtime.startedAt(),
                ageSeconds,
                coverageState,
                severity,
                List.copyOf(windows));
    }

    private WindowObservation observeWindow(
            List<XingweiRuntimeRequestMetrics.MinuteBucket> retained,
            Instant publishedAt,
            Instant now,
            int minutes,
            String coverageState) {
        if (!"COVERED".equals(coverageState)) {
            return WindowObservation.unavailable(minutes, coverageState);
        }

        Instant targetEnd = publishedAt.plus(minutes, ChronoUnit.MINUTES);
        Instant observedEnd = now.isBefore(targetEnd) ? now : targetEnd;
        Instant startMinute = publishedAt.truncatedTo(ChronoUnit.MINUTES);
        Instant endExclusive = observedEnd.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES);

        long total = 0L;
        long clients = 0L;
        long servers = 0L;
        for (XingweiRuntimeRequestMetrics.MinuteBucket bucket : retained) {
            if (bucket.minute().isBefore(startMinute) || !bucket.minute().isBefore(endExclusive)) {
                continue;
            }
            total += Math.max(0L, bucket.totalRequests());
            clients += Math.max(0L, bucket.clientErrors());
            servers += Math.max(0L, bucket.serverErrors());
        }

        double errorRate = total > 0 ? (double) servers / (double) total : 0.0;
        String state = now.isBefore(targetEnd) ? "IN_PROGRESS" : "COMPLETE";
        long observedSeconds = Math.max(0L, Duration.between(publishedAt, observedEnd).getSeconds());
        int observedMinutes = (int) Math.min(minutes, Math.max(0L, (observedSeconds + 59L) / 60L));
        return new WindowObservation(
                minutes,
                state,
                observedMinutes,
                total,
                clients,
                servers,
                errorRate,
                severity(total, servers, errorRate));
    }

    private String severity(long total, long servers, double errorRate) {
        boolean criticalByRate = total >= RATE_MIN_REQUESTS && errorRate >= CRITICAL_RATE;
        boolean warningByRate = total >= RATE_MIN_REQUESTS && errorRate >= WARNING_RATE;
        if (servers >= CRITICAL_COUNT || criticalByRate) {
            return "CRITICAL";
        }
        if (servers >= WARNING_COUNT || warningByRate) {
            return "WARNING";
        }
        if (servers > 0) {
            return "WATCH";
        }
        return "OK";
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 3;
            case "WARNING" -> 2;
            case "WATCH" -> 1;
            default -> 0;
        };
    }

    public record Snapshot(
            Instant sampledAt,
            CurrentObservation current,
            boolean historyAvailable,
            String historyStatus,
            XingweiReleaseHistoryService.Baseline baseline,
            List<XingweiReleaseHistoryService.Entry> history,
            int recheckAfterSeconds,
            int processRetentionMinutes) {}

    public record CurrentObservation(
            boolean available,
            String status,
            Instant publishedAt,
            Instant runtimeStartedAt,
            long ageSeconds,
            String coverageState,
            String severity,
            List<WindowObservation> windows) {
        static CurrentObservation unavailable(String status, Instant runtimeStartedAt) {
            return new CurrentObservation(
                    false,
                    status,
                    null,
                    runtimeStartedAt,
                    0L,
                    "UNAVAILABLE",
                    "UNKNOWN",
                    List.of());
        }
    }

    public record WindowObservation(
            int minutes,
            String state,
            int observedMinutes,
            long totalRequests,
            long clientErrors,
            long serverErrors,
            double serverErrorRate,
            String severity) {
        static WindowObservation unavailable(int minutes, String state) {
            return new WindowObservation(minutes, state, 0, 0, 0, 0, 0.0, "UNKNOWN");
        }
    }
}
