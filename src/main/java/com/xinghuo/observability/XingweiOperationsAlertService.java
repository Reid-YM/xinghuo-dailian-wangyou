package com.xinghuo.observability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 将应用运行真值与主机发布凭证组合成只读的稳定运营告警摘要。
 *
 * <p>该服务不执行部署、回滚、重启或业务写入。告警只基于聚合指标与发布凭证，
 * 不读取 URL、账号、IP、订单号、请求体或异常文本。
 */
@Service
public class XingweiOperationsAlertService {
    static final int RECENT_WINDOW_MINUTES = 5;
    static final int DB_WATCH_MILLIS = 250;
    static final int DB_WARNING_MILLIS = 1_000;
    static final double HEAP_WATCH_PERCENT = 80.0;
    static final double HEAP_WARNING_PERCENT = 90.0;
    static final double HEAP_CRITICAL_PERCENT = 97.0;
    static final long SERVER_WARNING_COUNT = 3L;
    static final long SERVER_CRITICAL_COUNT = 10L;
    static final long SERVER_RATE_MIN_REQUESTS = 50L;
    static final double SERVER_WARNING_RATE = 0.03;
    static final double SERVER_CRITICAL_RATE = 0.10;
    static final int RELEASE_LATENCY_WATCH_DELTA_MILLIS = 500;
    static final double RELEASE_LATENCY_WATCH_RATIO = 2.0;

    private final XingweiRuntimeHealthService runtimeHealthService;
    private final XingweiReleaseAttestationService releaseAttestationService;

    public XingweiOperationsAlertService(
            XingweiRuntimeHealthService runtimeHealthService,
            XingweiReleaseAttestationService releaseAttestationService) {
        this.runtimeHealthService = runtimeHealthService;
        this.releaseAttestationService = releaseAttestationService;
    }

    public Snapshot snapshot() {
        XingweiRuntimeHealthService.Snapshot runtime = runtimeHealthService.snapshot();
        XingweiReleaseAttestationService.Snapshot release = releaseAttestationService.snapshot();
        List<Alert> alerts = new ArrayList<>();

        WindowStats recent = recentWindow(runtime.requests().recentMinutes());
        double heapPercent = percent(runtime.jvm().usedHeapBytes(), runtime.jvm().maxHeapBytes());
        double serverErrorRate = recent.totalRequests() > 0
                ? (double) recent.serverErrors() / (double) recent.totalRequests()
                : 0.0;

        assessDatabase(runtime.database(), alerts);
        assessHeap(heapPercent, alerts);
        assessServerErrors(recent, serverErrorRate, alerts);

        ReleaseComparison releaseComparison = assessRelease(release, alerts);
        alerts.sort(Comparator.comparingInt((Alert alert) -> alert.level().rank()).reversed());
        Level overall = alerts.stream()
                .map(Alert::level)
                .max(Comparator.comparingInt(Level::rank))
                .orElse(Level.OK);

        return new Snapshot(
                runtime.sampledAt(),
                overall.name(),
                overall.label(),
                alerts.size(),
                List.copyOf(alerts),
                new RuntimeSummary(
                        runtime.database().up(),
                        runtime.database().checkMillis(),
                        heapPercent,
                        recent.totalRequests(),
                        recent.serverErrors(),
                        serverErrorRate,
                        RECENT_WINDOW_MINUTES),
                releaseComparison,
                thresholds(),
                60);
    }

    private void assessDatabase(
            XingweiRuntimeHealthService.DatabaseSnapshot database,
            List<Alert> alerts) {
        if (!database.up()) {
            alerts.add(new Alert(
                    "database-down",
                    Level.CRITICAL,
                    "数据库连通检查失败",
                    "当前 JVM 的 SELECT 1 连通检查未通过。",
                    "先核对数据库服务与连接配置，再复核运行健康；不要直接执行发布或回滚。",
                    "APPLICATION"));
            return;
        }
        if (database.checkMillis() >= DB_WARNING_MILLIS) {
            alerts.add(new Alert(
                    "database-latency-warning",
                    Level.WARNING,
                    "数据库检查明显变慢",
                    "SELECT 1 耗时达到 " + database.checkMillis() + " ms。",
                    "检查数据库负载、连接池与网络延迟，1 分钟后再次复核。",
                    "APPLICATION"));
        } else if (database.checkMillis() >= DB_WATCH_MILLIS) {
            alerts.add(new Alert(
                    "database-latency-watch",
                    Level.WATCH,
                    "数据库检查延迟升高",
                    "SELECT 1 耗时达到 " + database.checkMillis() + " ms。",
                    "继续观察下一次采样；若持续升高再检查数据库负载与连接池。",
                    "APPLICATION"));
        }
    }

    private void assessHeap(double heapPercent, List<Alert> alerts) {
        if (heapPercent >= HEAP_CRITICAL_PERCENT) {
            alerts.add(new Alert(
                    "heap-critical",
                    Level.CRITICAL,
                    "JVM 堆内存接近上限",
                    String.format("当前堆占用约 %.1f%%。", heapPercent),
                    "优先检查 GC、对象增长和近期流量，不在页面执行重启；必要时按主机运维流程处理。",
                    "APPLICATION"));
        } else if (heapPercent >= HEAP_WARNING_PERCENT) {
            alerts.add(new Alert(
                    "heap-warning",
                    Level.WARNING,
                    "JVM 堆内存偏高",
                    String.format("当前堆占用约 %.1f%%。", heapPercent),
                    "观察后续采样并检查 GC 与内存增长趋势。",
                    "APPLICATION"));
        } else if (heapPercent >= HEAP_WATCH_PERCENT) {
            alerts.add(new Alert(
                    "heap-watch",
                    Level.WATCH,
                    "JVM 堆内存进入观察区",
                    String.format("当前堆占用约 %.1f%%。", heapPercent),
                    "继续观察下一次采样；若达到 90% 再升级处理。",
                    "APPLICATION"));
        }
    }

    private void assessServerErrors(WindowStats recent, double rate, List<Alert> alerts) {
        boolean criticalByRate = recent.totalRequests() >= SERVER_RATE_MIN_REQUESTS && rate >= SERVER_CRITICAL_RATE;
        boolean warningByRate = recent.totalRequests() >= SERVER_RATE_MIN_REQUESTS && rate >= SERVER_WARNING_RATE;
        if (recent.serverErrors() >= SERVER_CRITICAL_COUNT || criticalByRate) {
            alerts.add(new Alert(
                    "server-errors-critical",
                    Level.CRITICAL,
                    "近 5 分钟 5xx 达到严重阈值",
                    formatServerWindow(recent, rate),
                    "先核对最近 5xx 时间、应用日志和依赖状态；确认影响面后再决定是否需要回滚。",
                    "APPLICATION"));
        } else if (recent.serverErrors() >= SERVER_WARNING_COUNT || warningByRate) {
            alerts.add(new Alert(
                    "server-errors-warning",
                    Level.WARNING,
                    "近 5 分钟 5xx 持续出现",
                    formatServerWindow(recent, rate),
                    "检查最近 5xx 对应的应用日志与依赖状态，并在 1 分钟后复核趋势。",
                    "APPLICATION"));
        } else if (recent.serverErrors() > 0) {
            alerts.add(new Alert(
                    "server-errors-watch",
                    Level.WATCH,
                    "近 5 分钟出现 5xx",
                    formatServerWindow(recent, rate),
                    "先观察下一次采样；若累计达到 3 次或错误率继续升高则升级处理。",
                    "APPLICATION"));
        }
    }

    private ReleaseComparison assessRelease(
            XingweiReleaseAttestationService.Snapshot release,
            List<Alert> alerts) {
        if (!release.available() || release.attestation() == null) {
            alerts.add(new Alert(
                    "release-attestation-unavailable",
                    Level.WARNING,
                    "生产发布凭证不可验证",
                    "主机侧发布凭证状态为 " + release.status() + "。",
                    "结合 Deploy Production 与服务器现场核验；该状态本身不等同于生产故障。",
                    "HOST_ATTESTATION"));
            return ReleaseComparison.unavailable(release.status());
        }

        XingweiReleaseAttestationService.Attestation attestation = release.attestation();
        if (attestation.healthComparison() == null) {
            alerts.add(new Alert(
                    "release-health-comparison-pending",
                    Level.WATCH,
                    "发布前后健康对比尚未建立",
                    "当前仍是 schema v" + attestation.schemaVersion() + " 发布凭证。",
                    "完成下一次受控生产发布后将自动生成发布前后 8080 健康基线。",
                    "HOST_ATTESTATION"));
            return new ReleaseComparison(false, "PENDING", 0, 0, 0, 0, 0, 0);
        }

        XingweiReleaseAttestationService.HealthProbe before = attestation.healthComparison().preDeploy8080();
        XingweiReleaseAttestationService.HealthProbe after = attestation.healthComparison().postDeploy8080();
        int beforeMax = Math.max(before.homeMillis(), before.servicesMillis());
        int afterMax = Math.max(after.homeMillis(), after.servicesMillis());
        int delta = afterMax - beforeMax;
        double ratio = beforeMax > 0 ? (double) afterMax / (double) beforeMax : (afterMax > 0 ? Double.POSITIVE_INFINITY : 1.0);
        boolean degraded = delta >= RELEASE_LATENCY_WATCH_DELTA_MILLIS && ratio >= RELEASE_LATENCY_WATCH_RATIO;

        if (degraded) {
            alerts.add(new Alert(
                    "release-latency-watch",
                    Level.WATCH,
                    "发布后 8080 探针较发布前变慢",
                    "发布前最慢探针 " + beforeMax + " ms，发布后 " + afterMax + " ms。",
                    "结合当前请求趋势继续观察；若 5xx、数据库或堆内存同时异常，再升级处理。",
                    "HOST_ATTESTATION"));
        }

        return new ReleaseComparison(
                true,
                degraded ? "SLOWER" : "STABLE",
                before.homeMillis(),
                before.servicesMillis(),
                after.homeMillis(),
                after.servicesMillis(),
                delta,
                finiteRatio(ratio));
    }

    private WindowStats recentWindow(List<XingweiRuntimeRequestMetrics.MinuteBucket> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            return new WindowStats(0L, 0L);
        }
        int from = Math.max(0, buckets.size() - RECENT_WINDOW_MINUTES);
        long requests = 0L;
        long serverErrors = 0L;
        for (int index = from; index < buckets.size(); index++) {
            XingweiRuntimeRequestMetrics.MinuteBucket bucket = buckets.get(index);
            requests += Math.max(0L, bucket.totalRequests());
            serverErrors += Math.max(0L, bucket.serverErrors());
        }
        return new WindowStats(requests, serverErrors);
    }

    private double percent(long used, long max) {
        if (used < 0 || max <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, ((double) used / (double) max) * 100.0));
    }

    private double finiteRatio(double value) {
        return Double.isFinite(value) ? value : 999.0;
    }

    private String formatServerWindow(WindowStats recent, double rate) {
        return String.format(
                "近 %d 分钟 %d 次请求中出现 %d 次 5xx，错误率约 %.2f%%。",
                RECENT_WINDOW_MINUTES,
                recent.totalRequests(),
                recent.serverErrors(),
                rate * 100.0);
    }

    private Thresholds thresholds() {
        return new Thresholds(
                DB_WATCH_MILLIS,
                DB_WARNING_MILLIS,
                HEAP_WATCH_PERCENT,
                HEAP_WARNING_PERCENT,
                HEAP_CRITICAL_PERCENT,
                SERVER_WARNING_COUNT,
                SERVER_CRITICAL_COUNT,
                SERVER_RATE_MIN_REQUESTS,
                SERVER_WARNING_RATE,
                SERVER_CRITICAL_RATE,
                RELEASE_LATENCY_WATCH_DELTA_MILLIS,
                RELEASE_LATENCY_WATCH_RATIO);
    }

    enum Level {
        OK(0, "正常"),
        WATCH(1, "观察"),
        WARNING(2, "警告"),
        CRITICAL(3, "严重");

        private final int rank;
        private final String label;

        Level(int rank, String label) {
            this.rank = rank;
            this.label = label;
        }

        int rank() {
            return rank;
        }

        String label() {
            return label;
        }
    }

    record WindowStats(long totalRequests, long serverErrors) {}

    public record Snapshot(
            Instant sampledAt,
            String severity,
            String severityLabel,
            int activeAlertCount,
            List<Alert> alerts,
            RuntimeSummary runtime,
            ReleaseComparison releaseComparison,
            Thresholds thresholds,
            int recheckAfterSeconds) {}

    public record Alert(
            String code,
            Level level,
            String title,
            String detail,
            String recommendedAction,
            String source) {}

    public record RuntimeSummary(
            boolean databaseUp,
            long databaseCheckMillis,
            double heapPercent,
            long recentRequests,
            long recentServerErrors,
            double recentServerErrorRate,
            int windowMinutes) {}

    public record ReleaseComparison(
            boolean available,
            String state,
            int beforeHomeMillis,
            int beforeServicesMillis,
            int afterHomeMillis,
            int afterServicesMillis,
            int maxDeltaMillis,
            double maxRatio) {
        static ReleaseComparison unavailable(String state) {
            return new ReleaseComparison(false, state, 0, 0, 0, 0, 0, 0);
        }
    }

    public record Thresholds(
            int databaseWatchMillis,
            int databaseWarningMillis,
            double heapWatchPercent,
            double heapWarningPercent,
            double heapCriticalPercent,
            long serverWarningCount5m,
            long serverCriticalCount5m,
            long serverRateMinRequests,
            double serverWarningRate,
            double serverCriticalRate,
            int releaseLatencyWatchDeltaMillis,
            double releaseLatencyWatchRatio) {}
}
