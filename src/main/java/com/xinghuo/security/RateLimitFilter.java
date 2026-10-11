package com.xinghuo.security;

import com.xinghuo.util.RequestUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value=-2147483628)
public class RateLimitFilter
extends OncePerRequestFilterBase {
    private final ConcurrentHashMap<String, Window> map = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();

    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String p = req.getRequestURI();
        int max = p.startsWith("/api/records/") || p.startsWith("/api/orders/") ? 30 : (p.equals("/api/quote") || p.equals("/api/uploads/quote") ? 12 : 0);
        if (max > 0) {
            Window w;
            if (this.requests.incrementAndGet() % 500L == 0L || this.map.size() > 5000) {
                this.cleanup();
            }
            String key = RequestUtil.clientIp((HttpServletRequest)req) + "|" + p.replaceAll("XH(?:[A-Z0-9]{10}|-[A-Z2-9]{24})", "record");
            Window window = w = this.map.compute(key, (k, v) -> v == null || v.expired() ? new Window() : v);
            synchronized (window) {
                if (++w.count > max) {
                    res.setStatus(429);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\"}");
                    return;
                }
            }
        }
        chain.doFilter((ServletRequest)req, (ServletResponse)res);
    }

    private void cleanup() {
        this.map.entrySet().removeIf(e -> e.getValue().expired());
    }

    private static class Window {
        long start = System.currentTimeMillis();
        int count = 0;

        private Window() {
        }

        boolean expired() {
            return System.currentTimeMillis() - this.start > 60000L;
        }
    }
}
