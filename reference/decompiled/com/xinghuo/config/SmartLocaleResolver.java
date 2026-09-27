/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.config.SmartLocaleResolver
 *  com.xinghuo.config.WebI18nConfig
 *  com.xinghuo.security.GeoLanguageResolver
 *  jakarta.servlet.http.Cookie
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  lombok.Generated
 *  org.springframework.web.servlet.LocaleResolver
 */
package com.xinghuo.config;

import com.xinghuo.config.WebI18nConfig;
import com.xinghuo.security.GeoLanguageResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import lombok.Generated;
import org.springframework.web.servlet.LocaleResolver;

public class SmartLocaleResolver
implements LocaleResolver {
    private static final String RESOLVED_ATTR = SmartLocaleResolver.class.getName() + ".resolved";
    private final GeoLanguageResolver geo;

    public Locale resolveLocale(HttpServletRequest request) {
        Object cached = request.getAttribute(RESOLVED_ATTR);
        if (cached instanceof Locale) {
            Locale locale = (Locale)cached;
            return locale;
        }
        String explicit = request.getParameter("lang");
        if (explicit != null && !explicit.isBlank()) {
            return this.remember(request, WebI18nConfig.normalize((String)explicit));
        }
        String cookie = this.cookieValue(request, "xh_lang");
        if (cookie != null && !cookie.isBlank()) {
            return this.remember(request, WebI18nConfig.normalize((String)cookie));
        }
        String byIp = this.geo.detectLanguage(this.clientIp(request));
        if (byIp != null) {
            return this.remember(request, WebI18nConfig.normalize((String)byIp));
        }
        return this.remember(request, this.fromAcceptLanguage(request));
    }

    private Locale remember(HttpServletRequest request, Locale locale) {
        request.setAttribute(RESOLVED_ATTR, (Object)locale);
        return locale;
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (locale == null || response == null) {
            return;
        }
        Locale normalized = WebI18nConfig.normalize((String)locale.toLanguageTag());
        Cookie cookie = new Cookie("xh_lang", normalized.toLanguageTag());
        cookie.setPath("/");
        cookie.setMaxAge(31536000);
        cookie.setHttpOnly(false);
        cookie.setSecure(request != null && request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public void persistIfAbsent(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (this.cookieValue(request, "xh_lang") == null) {
            this.setLocale(request, response, locale);
        }
    }

    private Locale fromAcceptLanguage(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header == null || header.isBlank()) {
            return WebI18nConfig.DEFAULT_LOCALE;
        }
        for (String part : header.split(",")) {
            Locale candidate;
            String tag = part.split(";")[0].trim();
            if (tag.isBlank() || !(candidate = WebI18nConfig.normalize((String)tag)).getLanguage().equals(Locale.ENGLISH.getLanguage()) && !candidate.getLanguage().equals(Locale.CHINESE.getLanguage())) continue;
            return candidate;
        }
        return WebI18nConfig.DEFAULT_LOCALE;
    }

    private String clientIp(HttpServletRequest request) {
        String first;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (!(forwarded == null || forwarded.isBlank() || (first = forwarded.split(",")[0].trim()).isBlank() || this.isPrivate(first))) {
            return first;
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank() && !this.isPrivate(real.trim())) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isPrivate(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("127.") || ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.") || ip.startsWith("172.19.") || ip.startsWith("172.2") || ip.startsWith("172.30.") || ip.startsWith("172.31.") || ip.startsWith("169.254.") || ip.equals("::1");
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (!name.equals(c.getName())) continue;
            return c.getValue();
        }
        return null;
    }

    @Generated
    public SmartLocaleResolver(GeoLanguageResolver geo) {
        this.geo = geo;
    }
}

