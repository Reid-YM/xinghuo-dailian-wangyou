package com.xinghuo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Routes public frontend pages to an independent mobile template tree.
 * Desktop keeps the existing templates; mobile resolves to templates/mobile/*.
 * Admin/API/static resources are intentionally excluded from the mobile split.
 */
public final class FrontendDeviceViewInterceptor implements HandlerInterceptor {
    private static final Set<String> MOBILE_VIEWS = Set.of(
            "index", "services", "service-detail", "quote", "records", "record-detail",
            "order", "order-detail", "contact", "trust", "rules", "rules-en",
            "privacy", "privacy-en", "guides", "guide", "game", "mw4-camo-tracker",
            "account-login", "account-register", "account-reset", "account", "account-settings",
            "account-submit", "account-orders", "account-progress", "account-requests", "console", "error"
    );

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        boolean mobile = isMobile(request);
        request.setAttribute("frontendMode", mobile ? "mobile" : "desktop");
        if (!mobile || modelAndView == null) return;
        String view = modelAndView.getViewName();
        if (view == null || view.startsWith("redirect:") || view.startsWith("forward:")) return;
        if (view.startsWith("mobile/") || !MOBILE_VIEWS.contains(view)) return;
        modelAndView.setViewName("mobile/" + view);
    }

    static boolean isMobile(HttpServletRequest request) {
        if (request == null) return false;
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/admin") || path.startsWith("/api/") || path.startsWith("/assets/") || path.startsWith("/uploads/"))) {
            return false;
        }

        // Header override is intentionally for tests/operations only; no alternate public URL is created.
        String forced = request.getHeader("X-Xinghuo-Frontend");
        if ("mobile".equalsIgnoreCase(forced)) return true;
        if ("desktop".equalsIgnoreCase(forced)) return false;

        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isBlank()) return false;
        String value = ua.toLowerCase(Locale.ROOT);
        if (value.contains("ipad") || value.contains("tablet")) return false;
        if (value.contains("iphone") || value.contains("ipod") || value.contains("windows phone")) return true;
        if (value.contains("android") && value.contains("mobile")) return true;
        if (value.contains("harmonyos") && (value.contains("mobile") || value.contains("phone"))) return true;
        return value.contains("mobi");
    }
}
