package com.xinghuo.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 收口已经退出主流程的公开页面。
 *
 * <p>旧链接可能仍存在于浏览器书签、搜索缓存或历史客服消息中，因此不直接 404：
 * 能映射到新功能的地址统一返回 308，确保 GET/HEAD 语义稳定且搜索引擎能收敛到新 URL。
 */
@Component
public class LegacyRouteRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String target = redirectTarget(request.getRequestURI());
        if (target == null || !("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.PERMANENT_REDIRECT.value());
        response.setHeader("Location", target);
        response.setHeader("Cache-Control", "no-store");
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
