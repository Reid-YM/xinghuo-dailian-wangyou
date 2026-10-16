package com.xinghuo.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 记录应用请求状态码，不采集路径、参数或业务内容。
 */
@Component
public class XingweiRuntimeMetricsFilter extends OncePerRequestFilter {
    private final XingweiRuntimeRequestMetrics metrics;

    public XingweiRuntimeMetricsFilter(XingweiRuntimeRequestMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/assets/") || "/favicon.ico".equals(uri);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
            metrics.record(response.getStatus());
        } catch (ServletException | IOException | RuntimeException exception) {
            metrics.record(Math.max(500, response.getStatus()));
            throw exception;
        }
    }
}
