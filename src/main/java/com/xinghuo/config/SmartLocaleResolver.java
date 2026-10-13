package com.xinghuo.config;

import com.xinghuo.security.GeoLanguageResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import lombok.Generated;
import org.springframework.web.servlet.LocaleResolver;

public class SmartLocaleResolver implements LocaleResolver {
    private static final String RESOLVED_ATTR = SmartLocaleResolver.class.getName() + ".resolved";
    private final GeoLanguageResolver geo;

    public Locale resolveLocale(HttpServletRequest request) {
        Object cached = request.getAttribute(RESOLVED_ATTR);
        if (cached instanceof Locale locale) {
            return locale;
        }

        // SEO canonical contract: these four legal URLs are real, separately indexable
        // language variants. Their rendered language must therefore be stable and must
        // never change because of IP, cookies or Accept-Language.
        Locale stablePathLocale = this.stablePathLocale(request.getRequestURI());
        if (stablePathLocale != null) {
            return this.remember(request, stablePathLocale);
        }

        // All other indexable public URLs currently have one canonical language version:
        // Chinese. Keep their rendered body deterministic even if a browser or crawler
        // carries an old xh_lang cookie or a lang query parameter. Serving English from
        // the same canonical URL is the locale-adaptive pattern that can make Google see
        // different content for one URL. Full English SEO should use dedicated /en/... URLs.
        if (this.isIndexablePublicPath(request.getRequestURI())) {
            return this.remember(request, WebI18nConfig.DEFAULT_LOCALE);
        }

        // Explicit user choice remains available on private/non-indexable utility pages.
        String explicit = request.getParameter("lang");
        if (explicit != null && !explicit.isBlank()) {
            return this.remember(request, WebI18nConfig.normalize(explicit));
        }
        String cookie = this.cookieValue(request, WebI18nConfig.COOKIE_NAME);
        if (cookie != null && !cookie.isBlank()) {
            return this.remember(request, WebI18nConfig.normalize(cookie));
        }

        // Non-indexable/private utility pages can keep the convenience auto-detection.
        String byIp = this.geo.detectLanguage(this.clientIp(request));
        if (byIp != null) {
            return this.remember(request, WebI18nConfig.normalize(byIp));
        }
        return this.remember(request, this.fromAcceptLanguage(request));
    }

    private Locale stablePathLocale(String uri) {
        if ("/rules".equals(uri) || "/privacy".equals(uri)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if ("/rules-en".equals(uri) || "/privacy-en".equals(uri)) {
            return Locale.ENGLISH;
        }
        return null;
    }

    private boolean isIndexablePublicPath(String uri) {
        if (uri == null || uri.isBlank()) {
            return false;
        }
        if ("/".equals(uri)
                || "/services".equals(uri)
                || "/guides".equals(uri)
                || "/mastery".equals(uri)
                || "/quote".equals(uri)
                || "/contact".equals(uri)
                || "/trust".equals(uri)) {
            return true;
        }
        return uri.startsWith("/service/")
                || uri.startsWith("/game/")
                || uri.startsWith("/guides/");
    }

    private boolean isStablePublicPath(String uri) {
        return this.stablePathLocale(uri) != null || this.isIndexablePublicPath(uri);
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
        Cookie cookie = new Cookie(WebI18nConfig.COOKIE_NAME, normalized.toLanguageTag());
        cookie.setPath("/");
        cookie.setMaxAge(31536000);
        cookie.setHttpOnly(false);
        cookie.setSecure(request != null && request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public void persistIfAbsent(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // A canonical public URL has a language chosen by SEO routing, not by the user.
        // Do not turn that forced locale into a year-long preference cookie, otherwise
        // an English visitor who merely opens /services would later inherit Chinese on
        // private/non-indexable pages where automatic or explicit language choice is valid.
        if (request != null && this.isStablePublicPath(request.getRequestURI())) {
            return;
        }
        if (this.cookieValue(request, WebI18nConfig.COOKIE_NAME) == null) {
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
            if (candidate.getLanguage().equals(Locale.ENGLISH.getLanguage())
                    || candidate.getLanguage().equals(Locale.CHINESE.getLanguage())) {
                return candidate;
            }
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
        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("127.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")
                || ip.startsWith("169.254.")
                || ip.equals("::1");
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Generated
    public SmartLocaleResolver(GeoLanguageResolver geo) {
        this.geo = geo;
    }
}
