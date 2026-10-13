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

/**
 * Keeps user-only utility pages out of the search index and permanently folds
 * historical order URLs into the current business-record URLs.
 */
@Component
public class PublicUtilitySeoFilter extends OncePerRequestFilter {

    private static final String NOINDEX = "noindex, follow";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if ("/order".equals(path)) {
                permanentRedirect(response, "/records");
                return;
            }
            if (path != null && path.startsWith("/order/")) {
                permanentRedirect(response, "/record/" + path.substring("/order/".length()));
                return;
            }
        }

        if ("/records".equals(path) || (path != null && path.startsWith("/record/"))) {
            response.setHeader("X-Robots-Tag", NOINDEX);
        }

        chain.doFilter((ServletRequest) request, (ServletResponse) response);
    }

    private void permanentRedirect(HttpServletResponse response, String target) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", target);
        response.setHeader("X-Robots-Tag", NOINDEX);
    }
}
