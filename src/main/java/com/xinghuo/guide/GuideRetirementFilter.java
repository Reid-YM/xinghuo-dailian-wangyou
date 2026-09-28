package com.xinghuo.guide;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GuideRetirementFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean browserRequest = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        if (browserRequest && (path.equals("/guides") || path.startsWith("/guides/"))) {
            response.sendRedirect("/services");
            return;
        }
        chain.doFilter(request, response);
    }
}
