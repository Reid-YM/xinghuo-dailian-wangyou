package com.xinghuo.observability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * 进程内请求健康基线。
 *
 * <p>只记录聚合状态码、最近一次 5xx 的时间/状态码，以及分钟级聚合趋势；
 * 对外运行健康仍展示最近 15 分钟，内部保留最近 60 分钟用于发布后 5/15/30 分钟观察。
 * 不保留 URL、查询参数、请求体、账号、IP 或业务编号。数据自当前 JVM 启动后累计，进程重启后自然清零。
 */
@Component
public class XingweiRuntimeRequestMetrics {
    static final int TREND_MINUTES = 15;
    static final int RETENTION_MINUTES = 60;
    private static final long MINUTE_MILLIS = 60_000L;

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong clientErrors = new AtomicLong();
    private final AtomicLong serverErrors = new AtomicLong();
    private final AtomicReference<RecentServerError> lastServerError = new AtomicReference<>();
    private final long[] bucketMinutes = new long[RETENTION_MINUTES];
    private final AtomicLongArray bucketTotals = new AtomicLongArray(RETENTION_MINUTES);
    private final AtomicLongArray bucketClientErrors = new AtomicLongArray(RETENTION_MINUTES);
    private final AtomicLongArray bucketServerErrors = new AtomicLongArray(RETENTION_MINUTES);
    private final Object[] bucketLocks = new Object[RETENTION_MINUTES];

    public XingweiRuntimeRequestMetrics() {
        for (int i = 0; i < RETENTION_MINUTES; i++) {
            bucketMinutes[i] = Long.MIN_VALUE;
            bucketLocks[i] = new Object();
        }
    }

    public void record(int status) {
        recordAt(status, System.currentTimeMillis());
    }

    void recordAt(int status, long atMillis) {
        totalRequests.incrementAndGet();
        if (status >= 500) {
            serverErrors.incrementAndGet();
            lastServerError.set(new RecentServerError(Instant.ofEpochMilli(atMillis), status));
        } else if (status >= 400) {
            clientErrors.incrementAndGet();
        }

        long epochMinute = Math.floorDiv(atMillis, MINUTE_MILLIS);
        int index = (int) Math.floorMod(epochMinute, (long) RETENTION_MINUTES);
        synchronized (bucketLocks[index]) {
            if (bucketMinutes[index] != epochMinute) {
                bucketMinutes[index] = epochMinute;
                bucketTotals.set(index, 0L);
                bucketClientErrors.set(index, 0L);
                bucketServerErrors.set(index, 0L);
            }
            bucketTotals.incrementAndGet(index);
            if (status >= 500) {
                bucketServerErrors.incrementAndGet(index);
            } else if (status >= 400) {
                bucketClientErrors.incrementAndGet(index);
            }
        }
    }

    public Snapshot snapshot() {
        long nowMillis = System.currentTimeMillis();
        return new Snapshot(
                totalRequests.get(),
                clientErrors.get(),
                serverErrors.get(),
                lastServerError.get(),
                recentMinutes(nowMillis));
    }

    List<MinuteBucket> recentMinutes(long nowMillis) {
        return minuteWindow(nowMillis, TREND_MINUTES);
    }

    List<MinuteBucket> retainedMinutes(long nowMillis) {
        return minuteWindow(nowMillis, RETENTION_MINUTES);
    }

    private List<MinuteBucket> minuteWindow(long nowMillis, int minutes) {
        int boundedMinutes = Math.max(1, Math.min(minutes, RETENTION_MINUTES));
        long currentMinute = Math.floorDiv(nowMillis, MINUTE_MILLIS);
        List<MinuteBucket> buckets = new ArrayList<>(boundedMinutes);
        for (int offset = boundedMinutes - 1; offset >= 0; offset--) {
            long epochMinute = currentMinute - offset;
            int index = (int) Math.floorMod(epochMinute, (long) RETENTION_MINUTES);
            long total = 0L;
            long clients = 0L;
            long servers = 0L;
            synchronized (bucketLocks[index]) {
                if (bucketMinutes[index] == epochMinute) {
                    total = bucketTotals.get(index);
                    clients = bucketClientErrors.get(index);
                    servers = bucketServerErrors.get(index);
                }
            }
            buckets.add(new MinuteBucket(
                    Instant.ofEpochMilli(epochMinute * MINUTE_MILLIS),
                    total,
                    clients,
                    servers));
        }
        return List.copyOf(buckets);
    }

    public record RecentServerError(Instant at, int status) {}

    public record MinuteBucket(
            Instant minute,
            long totalRequests,
            long clientErrors,
            long serverErrors) {}

    public record Snapshot(
            long totalRequests,
            long clientErrors,
            long serverErrors,
            RecentServerError lastServerError,
            List<MinuteBucket> recentMinutes) {}
}
