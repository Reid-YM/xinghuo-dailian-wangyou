package com.xinghuo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminNoIndexFilter
extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (this.isPrivateArea(path)) {
            response.setHeader("X-Robots-Tag", "noindex, nofollow, noarchive");
        }
        chain.doFilter((ServletRequest)request, (ServletResponse)response);
    }

    private boolean isPrivateArea(String path) {
        if (path == null) {
            return false;
        }
        return path.equals("/admin") || path.startsWith("/admin/") || path.equals("/login") || path.equals("/logout") || path.startsWith("/api/admin/");
    }
}

