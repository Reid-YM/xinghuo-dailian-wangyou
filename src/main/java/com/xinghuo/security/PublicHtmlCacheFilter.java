/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.security.PublicHtmlCacheFilter
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  org.springframework.stereotype.Component
 *  org.springframework.web.filter.OncePerRequestFilter
 */
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
public class PublicHtmlCacheFilter
extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        boolean get;
        String path = request.getRequestURI();
        boolean bl = get = "GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod());
        if (get && this.cacheableSeoAsset(path)) {
            response.setHeader("Cache-Control", "public, max-age=300, stale-while-revalidate=1800");
            chain.doFilter((ServletRequest)request, (ServletResponse)response);
            return;
        }
        if (get && this.cacheablePublicApi(path)) {
            response.setHeader("Cache-Control", "public, max-age=30, stale-while-revalidate=120");
            chain.doFilter((ServletRequest)request, (ServletResponse)response);
            return;
        }
        if (path.startsWith("/api") || path.startsWith("/admin") || path.equals("/login") || path.equals("/logout")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        } else if (("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod())) && this.likelyPublicHtml(path)) {
            response.setHeader("Cache-Control", "public, max-age=60, stale-while-revalidate=300");
        }
        chain.doFilter((ServletRequest)request, (ServletResponse)response);
    }

    private boolean cacheableSeoAsset(String path) {
        return path.equals("/robots.txt") || path.equals("/sitemap.xml") || path.equals("/favicon.ico") || path.equals("/favicon.svg") || path.equals("/apple-touch-icon.png") || path.equals("/og.png");
    }

    private boolean cacheablePublicApi(String path) {
        return path.equals("/api/site-config") || path.startsWith("/api/public/") || path.equals("/api/services") || path.equals("/api/catalog") || path.equals("/api/tool-station") || path.startsWith("/api/site-assets/");
    }

    private boolean likelyPublicHtml(String path) {
        return path.equals("/") || path.equals("/services") || path.startsWith("/service/") || path.startsWith("/game/") || path.equals("/quote") || path.equals("/records") || path.startsWith("/record/") || path.equals("/contact") || path.equals("/rules") || path.equals("/privacy") || path.equals("/trust") || path.equals("/mastery") || path.equals("/order") || path.startsWith("/order/");
    }
}
