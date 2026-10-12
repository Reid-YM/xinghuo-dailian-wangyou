package com.xinghuo.security;

import com.xinghuo.util.RequestUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Public endpoint rate limiting for anonymous quote and business-status traffic.
 *
 * <p>This filter intentionally keeps its existing fixed-window behavior and thresholds. The
 * account-domain {@code RateLimiter} is a separate sliding-window implementation; consolidation
 * must preserve the effective public limits before either layer is removed.
 */
@Component
@Order(-2147483628)
public class RateLimitFilter extends OncePerRequestFilterBase {
    static final int RECORD_REQUESTS_PER_MINUTE = 30;
    static final int QUOTE_REQUESTS_PER_MINUTE = 12;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final long CLEANUP_EVERY = 500L;
    private static final int MAX_BUCKETS_BEFORE_CLEANUP = 5_000;
    private static final Pattern BUSINESS_NO =
            Pattern.compile("XH(?:[A-Z0-9]{10}|-[A-Z2-9]{24})");

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int maxRequests = maxRequestsFor(path);
        if (maxRequests > 0 && !allow(request, path, maxRequests)) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"请求过于频繁，请稍后重试\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allow(HttpServletRequest request, String path, int maxRequests) {
        if (requests.incrementAndGet() % CLEANUP_EVERY == 0L
                || buckets.size() > MAX_BUCKETS_BEFORE_CLEANUP) {
            cleanup();
        }

        String key = RequestUtil.clientIp(request) + "|" + normalizePath(path);
        Window window = buckets.compute(
                key,
                (ignored, current) -> current == null || current.expired() ? new Window() : current);
        synchronized (window) {
            window.count += 1;
            return window.count <= maxRequests;
        }
    }

    private int maxRequestsFor(String path) {
        if (path.startsWith("/api/records/") || path.startsWith("/api/orders/")) {
            return RECORD_REQUESTS_PER_MINUTE;
        }
        if (path.equals("/api/quote") || path.equals("/api/uploads/quote")) {
            return QUOTE_REQUESTS_PER_MINUTE;
        }
        return 0;
    }

    private String normalizePath(String path) {
        return BUSINESS_NO.matcher(path).replaceAll("record");
    }

    private void cleanup() {
        buckets.entrySet().removeIf(entry -> entry.getValue().expired());
    }

    private static final class Window {
        private final long start = System.currentTimeMillis();
        private int count;

        private boolean expired() {
            return System.currentTimeMillis() - start > WINDOW_MILLIS;
        }
    }
}
