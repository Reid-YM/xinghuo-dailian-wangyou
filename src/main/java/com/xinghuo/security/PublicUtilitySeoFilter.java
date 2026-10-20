package com.xinghuo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Keeps user-only utility pages out of the search index and permanently folds
 * historical order URLs into the current business-record URLs.
 */
@Component
public class PublicUtilitySeoFilter extends OncePerRequestFilter {

    private static final String NOINDEX = "noindex, follow";
    // Align the first X-Robots-Tag header with the actual frontend mode.
    @Value("${app.frontend-next.default-enabled:false}")
    private boolean frontendNextEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String base = path.startsWith("/next/") ? "/next" : "";
        if (!base.isEmpty()) path = path.substring(5);

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if ("/order".equals(path)) {
                permanentRedirect(request, response, base + "/records");
                return;
            }
            if (path != null && path.startsWith("/order/")) {
                permanentRedirect(request, response, base + "/record/" + path.substring("/order/".length()));
                return;
            }
        }

        if ("/records".equals(path) || (path != null && path.startsWith("/record/"))) {
            boolean nextView = (!base.isEmpty() || frontendNextEnabled);
            response.setHeader("X-Robots-Tag", nextView ? "noindex,nofollow" : NOINDEX);
        }

        chain.doFilter((ServletRequest) request, (ServletResponse) response);
    }

    private void permanentRedirect(HttpServletRequest request, HttpServletResponse response, String target) {
        if (!frontendNextEnabled && "1".equals(request.getParameter("legacy"))) target += "?legacy=1";
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", target);
        response.setHeader("X-Robots-Tag", NOINDEX);
    }
}
