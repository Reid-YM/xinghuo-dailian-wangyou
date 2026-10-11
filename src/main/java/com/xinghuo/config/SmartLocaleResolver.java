package com.xinghuo.config;

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
            return this.remember(request, WebI18nConfig.normalize(explicit));
        }
        String cookie = this.cookieValue(request, "xh_lang");
        if (cookie != null && !cookie.isBlank()) {
            return this.remember(request, WebI18nConfig.normalize(cookie));
        }

        // Authenticated/control surfaces must never depend on an external geo
        // service. A blocked geo provider used to delay /admin and every admin
        // API request before the handler could run. Browser language is enough
        // here; public storefront pages may still use geo as a convenience.
        if (this.skipGeoLookup(request)) {
            return this.remember(request, this.fromAcceptLanguage(request));
        }

        String byIp = this.geo.detectLanguage(this.clientIp(request));
        if (byIp != null) {
            return this.remember(request, WebI18nConfig.normalize(byIp));
        }
        return this.remember(request, this.fromAcceptLanguage(request));
    }

    private boolean skipGeoLookup(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.equals("/login")
                || path.equals("/logout")
                || path.equals("/admin")
                || path.startsWith("/admin/")
                || path.equals("/account")
                || path.startsWith("/account/")
                || path.equals("/console")
                || path.startsWith("/console/")
                || path.startsWith("/api/");
    }

    private Locale remember(HttpServletRequest request, Locale locale) {
        request.setAttribute(RESOLVED_ATTR, locale);
        return locale;
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (locale == null || response == null) {
            return;
        }
        Locale normalized = WebI18nConfig.normalize(locale.toLanguageTag());
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
            String tag = part.split(";")[0].trim();
            if (tag.isBlank()) {
                continue;
            }
            Locale candidate = WebI18nConfig.normalize(tag);
            if (!candidate.getLanguage().equals(Locale.ENGLISH.getLanguage())
                    && !candidate.getLanguage().equals(Locale.CHINESE.getLanguage())) {
                continue;
            }
            return candidate;
        }
        return WebI18nConfig.DEFAULT_LOCALE;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isBlank() && !this.isPrivate(first)) {
                return first;
            }
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank() && !this.isPrivate(real.trim())) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isPrivate(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String value = ip.trim().toLowerCase(Locale.ROOT);
        if (value.equals("::1") || value.startsWith("fc") || value.startsWith("fd") || value.startsWith("fe80:")) {
            return true;
        }
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        int[] parts = new int[4];
        try {
            for (int i = 0; i < octets.length; i++) {
                parts[i] = Integer.parseInt(octets[i]);
                if (parts[i] < 0 || parts[i] > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        return parts[0] == 10
                || parts[0] == 127
                || (parts[0] == 172 && parts[1] >= 16 && parts[1] <= 31)
                || (parts[0] == 192 && parts[1] == 168)
                || (parts[0] == 169 && parts[1] == 254);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (!name.equals(c.getName())) {
                continue;
            }
            return c.getValue();
        }
        return null;
    }

    @Generated
    public SmartLocaleResolver(GeoLanguageResolver geo) {
        this.geo = geo;
    }
}
