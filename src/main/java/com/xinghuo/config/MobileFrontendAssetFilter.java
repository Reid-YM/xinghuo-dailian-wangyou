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

/** Injects assets that belong exclusively to the mobile frontend template set. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class MobileFrontendAssetFilter extends OncePerRequestFilter {
    private static final String MARKER = "<meta name=\"xh-frontend\" content=\"mobile\">";
    private static final String ASSETS = MARKER
            + "<link rel=\"stylesheet\" href=\"/assets/mobile-app.css?v=20261009m2\">"
            + "<link rel=\"stylesheet\" href=\"/assets/mobile-compact.css?v=20261009mc1\">"
            + "<link rel=\"stylesheet\" href=\"/assets/mobile-screenshot-fixes.css?v=20261009msf2\">"
            + "<link rel=\"stylesheet\" href=\"/assets/mobile-flat-overflow.css?v=20261009mfo1\">"
            + "<link rel=\"stylesheet\" href=\"/assets/mobile-runtime-fixes.css?v=20261009mrf1\">"
            + "<link rel=\"stylesheet\" href=\"/assets/mobile-home-v4.css?v=20261009mh5\">"
            + "<link rel=\"stylesheet\" href=\"/assets/mobile-home-hero-r4.css?v=20261010r4ui1\">"
            + "<script defer src=\"/assets/mobile-app.js?v=20261009m2\"></script>"
            + "<script defer src=\"/assets/mobile-visual-runtime.js?v=20261010mv2\"></script>";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return true;
        if (!FrontendDeviceViewInterceptor.isMobile(request)) return true;
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
