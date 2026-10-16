package com.xinghuo.observability;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * 进程内请求健康基线。
 *
 * <p>只记录聚合状态码和最近一次 5xx 的时间/状态码，不保留 URL、查询参数、请求体、账号或业务编号。
 * 数据自当前 JVM 启动后累计，进程重启后自然清零。
 */
@Component
public class XingweiRuntimeRequestMetrics {
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong clientErrors = new AtomicLong();
    private final AtomicLong serverErrors = new AtomicLong();
    private final AtomicReference<RecentServerError> lastServerError = new AtomicReference<>();

    public void record(int status) {
        totalRequests.incrementAndGet();
        if (status >= 500) {
            serverErrors.incrementAndGet();
            lastServerError.set(new RecentServerError(Instant.now(), status));
        } else if (status >= 400) {
            clientErrors.incrementAndGet();
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
                totalRequests.get(),
                clientErrors.get(),
                serverErrors.get(),
                lastServerError.get());
    }

    public record RecentServerError(Instant at, int status) {}

    public record Snapshot(
            long totalRequests,
            long clientErrors,
            long serverErrors,
            RecentServerError lastServerError) {}
}
