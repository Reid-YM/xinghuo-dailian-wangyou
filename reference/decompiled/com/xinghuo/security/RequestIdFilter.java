/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.security.RequestIdFilter
 *  com.xinghuo.security.RouteHealthMonitor
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 *  org.springframework.web.filter.OncePerRequestFilter
 */
package com.xinghuo.security;

import com.xinghuo.security.RouteHealthMonitor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
@Order(value=-2147483648)
public class RequestIdFilter
extends OncePerRequestFilter {
    public static final String HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final long SLOW_REQUEST_MS = 3000L;
    private final RouteHealthMonitor monitor;

    public RequestIdFilter(RouteHealthMonitor monitor) {
        this.monitor = monitor;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String requestId = incoming == null || incoming.isBlank() || incoming.length() > 64 ? UUID.randomUUID().toString().substring(0, 12) : incoming.trim();
        request.setAttribute(ATTRIBUTE, (Object)requestId);
        response.setHeader(HEADER, requestId);
        long startedAt = System.currentTimeMillis();
        try {
            chain.doFilter((ServletRequest)request, (ServletResponse)response);
        }
        catch (Throwable throwable) {
            long elapsed = System.currentTimeMillis() - startedAt;
            int status = response.getStatus();
            if (status == 404) {
                this.monitor.recordNotFound(request.getRequestURI());
            }
            if (status >= 500) {
                log.warn("request failed id={} {} {} status={} {}ms", new Object[]{requestId, request.getMethod(), request.getRequestURI(), status, elapsed});
            } else if (elapsed >= 3000L) {
                log.warn("slow request id={} {} {} status={} {}ms", new Object[]{requestId, request.getMethod(), request.getRequestURI(), status, elapsed});
            }
            throw throwable;
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        int status = response.getStatus();
        if (status == 404) {
            this.monitor.recordNotFound(request.getRequestURI());
        }
        if (status >= 500) {
            log.warn("request failed id={} {} {} status={} {}ms", new Object[]{requestId, request.getMethod(), request.getRequestURI(), status, elapsed});
        } else if (elapsed >= 3000L) {
            log.warn("slow request id={} {} {} status={} {}ms", new Object[]{requestId, request.getMethod(), request.getRequestURI(), status, elapsed});
        }
    }
}

