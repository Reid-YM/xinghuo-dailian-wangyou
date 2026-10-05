package com.xinghuo.security;

import com.xinghuo.security.RouteHealthMonitor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LegacyRedirectFilter
extends OncePerRequestFilter {
    private static final Map<String, String> PERMANENT_REDIRECTS = Map.of("/service/dmz-task", "/service/mw4-dmz", "/service/camo", "/service/camo-task", "/service/other", "/service/other-task", "/loadouts", "/mastery", "/loadouts/", "/mastery");
    private final RouteHealthMonitor monitor;

    public LegacyRedirectFilter(RouteHealthMonitor monitor) {
        this.monitor = monitor;
    }

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String target;
        String string = target = "GET".equalsIgnoreCase(request.getMethod()) ? (String)PERMANENT_REDIRECTS.get(request.getRequestURI()) : null;
        if (target == null) {
            chain.doFilter((ServletRequest)request, (ServletResponse)response);
            return;
        }
        String query = request.getQueryString();
        this.monitor.recordRedirect(request.getRequestURI(), target);
        response.setStatus(301);
        response.setHeader("Location", (String)(query == null ? target : target + "?" + query));
    }
}

