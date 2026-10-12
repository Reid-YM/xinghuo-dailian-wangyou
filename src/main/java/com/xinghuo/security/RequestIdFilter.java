package com.xinghuo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Integer.MIN_VALUE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final long SLOW_REQUEST_MS = 3000L;
    private static final int MAX_INCOMING_ID_LENGTH = 64;
    private final RouteHealthMonitor monitor;

    public RequestIdFilter(RouteHealthMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER));
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader(HEADER, requestId);

        long startedAt = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            observeResult(request, response, requestId, startedAt);
        }
    }

    private String resolveRequestId(String incoming) {
        if (incoming == null || incoming.isBlank() || incoming.length() > MAX_INCOMING_ID_LENGTH) {
            return UUID.randomUUID().toString().substring(0, 12);
        }
        return incoming.trim();
    }

    private void observeResult(
            HttpServletRequest request,
            HttpServletResponse response,
            String requestId,
            long startedAt) {
        long elapsed = System.currentTimeMillis() - startedAt;
        int status = response.getStatus();
        if (status == 404) {
            monitor.recordNotFound(request.getRequestURI());
        }
        if (status >= 500) {
            log.warn(
                    "request failed id={} {} {} status={} {}ms",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    elapsed);
        } else if (elapsed >= SLOW_REQUEST_MS) {
            log.warn(
                    "slow request id={} {} {} status={} {}ms",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    elapsed);
        }
    }
}
