package com.xinghuo.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 收口已经退出主流程的公开页面与旧 API。
 *
 * <p>旧页面可能仍存在于浏览器书签、搜索缓存或历史客服消息中，因此能映射到新功能的
 * 地址统一返回 308。已经下线且没有等价 API 的接口直接返回 410，避免继续暴露兼容面。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LegacyRouteRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isRetiredApi(uri)) {
            response.setStatus(HttpStatus.GONE.value());
            response.setHeader("Cache-Control", "no-store");
            return;
        }

        String target = redirectTarget(uri);
        if (target == null || !("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.PERMANENT_REDIRECT.value());
        response.setHeader("Location", target);
        response.setHeader("Cache-Control", "no-store");
    }

    static boolean isRetiredApi(String uri) {
        return "/api/loadouts".equals(uri) || (uri != null && uri.startsWith("/api/loadouts/"));
    }

    static String redirectTarget(String uri) {
        if (uri == null) return null;
        if ("/order".equals(uri) || "/order/".equals(uri)) return "/records";
        if (uri.startsWith("/order/") && uri.length() > "/order/".length()) {
            return "/record/" + uri.substring("/order/".length());
        }
        if ("/loadouts".equals(uri) || "/loadouts/".equals(uri)) return "/mastery";
        return null;
    }
}
