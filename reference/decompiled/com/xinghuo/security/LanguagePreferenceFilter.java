/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.security.LanguagePreferenceFilter
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.Cookie
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LanguagePreferenceFilter
extends OncePerRequestFilter {
    private static final String COOKIE_NAME = "xh_lang";
    private static final Set<String> ZH_REGION_CODES = Set.of("CN", "HK", "MO", "TW", "SG");

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter((ServletRequest)request, (ServletResponse)response);
    }

    private boolean shouldAutoRoute(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (!"/".equals(request.getRequestURI())) {
            return false;
        }
        if (request.getParameter("lang") != null || this.hasLanguageCookie(request)) {
            return false;
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null || !userAgent.toLowerCase(Locale.ROOT).contains("bot");
    }

    private boolean preferredEnglish(HttpServletRequest request) {
        String country = this.firstHeader(request, new String[]{"CF-IPCountry", "CloudFront-Viewer-Country", "X-Vercel-IP-Country", "X-Appengine-Country", "X-Country-Code"});
        if (country != null && !country.isBlank() && !"XX".equalsIgnoreCase(country)) {
            return !ZH_REGION_CODES.contains(country.trim().toUpperCase(Locale.ROOT));
        }
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return false;
        }
        String lower = acceptLanguage.toLowerCase(Locale.ROOT);
        return !lower.startsWith("zh") && lower.contains("en");
    }

    private boolean hasLanguageCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (!COOKIE_NAME.equals(cookie.getName())) continue;
            return true;
        }
        return false;
    }

    private String firstHeader(HttpServletRequest request, String ... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (value == null || value.isBlank()) continue;
            return value;
        }
        return null;
    }
}

