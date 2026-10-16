package com.xinghuo.observability;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class XingweiRuntimeHealthService {
    private final JdbcTemplate jdbcTemplate;
    private final XingweiRuntimeRequestMetrics requestMetrics;
    private final long startedAtMillis;

    public XingweiRuntimeHealthService(
            JdbcTemplate jdbcTemplate,
            XingweiRuntimeRequestMetrics requestMetrics) {
        this.jdbcTemplate = jdbcTemplate;
        this.requestMetrics = requestMetrics;
        this.startedAtMillis = ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    public Snapshot snapshot() {
        long sampledAtMillis = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        long usedHeapBytes = Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
        long maxHeapBytes = runtime.maxMemory();
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        DatabaseSnapshot database = checkDatabase();

        return new Snapshot(
                Instant.ofEpochMilli(sampledAtMillis),
                Instant.ofEpochMilli(startedAtMillis),
                Math.max(0L, sampledAtMillis - startedAtMillis),
                database,
                new JvmSnapshot(usedHeapBytes, maxHeapBytes, threadCount),
                requestMetrics.snapshot());
    }

    private DatabaseSnapshot checkDatabase() {
        long started = System.nanoTime();
        boolean up = false;
        try {
            Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            up = value != null && value == 1;
        } catch (RuntimeException ignored) {
            // 诊断接口只返回连通状态，不外泄 JDBC URL、账号、SQL 异常或数据库实现细节。
        }
        long elapsedMillis = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
        return new DatabaseSnapshot(up, elapsedMillis);
    }

    public record Snapshot(
            Instant sampledAt,
            Instant startedAt,
            long uptimeMillis,
            DatabaseSnapshot database,
            JvmSnapshot jvm,
            XingweiRuntimeRequestMetrics.Snapshot requests) {}

    public record DatabaseSnapshot(boolean up, long checkMillis) {}

    public record JvmSnapshot(long usedHeapBytes, long maxHeapBytes, int threadCount) {}
}
