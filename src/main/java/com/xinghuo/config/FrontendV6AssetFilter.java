package com.xinghuo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Adds the V6 presentation layer to every public HTML page.
 *
 * This is deliberately presentation-only: no controller, DTO, API, database or business-flow changes.
 * It runs outside the mobile asset filter so the V6 stylesheets are emitted after legacy/mobile styles and
 * can act as the single visual source of truth while retaining the existing functional DOM.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class FrontendV6AssetFilter extends OncePerRequestFilter {
    private static final String MARKER = "<meta name=\"xh-ui\" content=\"v6\">";
    private static final String ASSETS = MARKER
            + "<script>document.documentElement.dataset.xhUi='v6';</script>"
            + "<link rel=\"stylesheet\" href=\"/assets/frontend-v6.css?v=20261009v6a\">"
            + "<link rel=\"stylesheet\" href=\"/assets/frontend-v6-geometry.css?v=20261009v6b\">"
            + "<script defer src=\"/assets/frontend-v6.js?v=20261009v6a\"></script>";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return true;
        String uri = request.getRequestURI();
        return uri == null || uri.startsWith("/admin") || uri.startsWith("/api/")
                || uri.startsWith("/assets/") || uri.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        chain.doFilter((ServletRequest) request, (ServletResponse) wrapper);

        String contentType = wrapper.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
            wrapper.copyBodyToResponse();
            return;
        }

        Charset charset = StandardCharsets.UTF_8;
        String encoding = wrapper.getCharacterEncoding();
        if (encoding != null && !encoding.isBlank()) {
            try { charset = Charset.forName(encoding); } catch (Exception ignored) { }
        }

        String html = new String(wrapper.getContentAsByteArray(), charset);
        if (!html.contains(MARKER)) {
            int headEnd = html.toLowerCase(Locale.ROOT).indexOf("</head>");
            if (headEnd >= 0) html = html.substring(0, headEnd) + ASSETS + html.substring(headEnd);
        }

        byte[] output = html.getBytes(charset);
        wrapper.resetBuffer();
        wrapper.getOutputStream().write(output);
        wrapper.setContentLength(output.length);
        wrapper.copyBodyToResponse();
    }
}
