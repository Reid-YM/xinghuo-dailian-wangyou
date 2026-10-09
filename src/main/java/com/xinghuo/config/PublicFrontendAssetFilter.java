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
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Public frontend visual boundary. Admin and API routes are intentionally excluded. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class PublicFrontendAssetFilter extends OncePerRequestFilter {
    static final String MARKER = "<meta name=\"xh-public-ui\" content=\"nova\">";
    static final String NOVA_CORE = "<link rel=\"stylesheet\" href=\"/assets/nova-ui.css?v=20261009nova01\">";
    static final String NOVA_PAGES = "<link rel=\"stylesheet\" href=\"/assets/nova-pages.css?v=20261009nova01\">";
    static final String NOVA_RESPONSIVE = "<link rel=\"stylesheet\" href=\"/assets/nova-responsive.css?v=20261009nova01\">";
    static final String NOVA_SHELL = "<script defer src=\"/assets/nova-shell.js?v=20261009nova01\"></script>";

    private static final Pattern LEGACY_ASSET_STYLES = Pattern.compile(
            "(?is)<link\\b(?=[^>]*\\brel\\s*=\\s*['\"]stylesheet['\"])(?=[^>]*\\bhref\\s*=\\s*['\"]/assets/(?!nova-(?:ui|pages|responsive)\\.css)[^'\"]+\\.css(?:\\?[^'\"]*)?['\"])[^>]*>");

    private static final Pattern LEGACY_VISUAL_SCRIPTS = Pattern.compile(
            "(?is)<script\\b[^>]*\\bsrc\\s*=\\s*['\"]/assets/(?:site-shell|mobile-app|mobile-visual-runtime|ui-refresh|ui-fixes|account-center-refine)\\.js(?:\\?[^'\"]*)?['\"][^>]*>\\s*</script>");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return true;
        String uri = request.getRequestURI();
        if (uri == null) return true;
        return uri.startsWith("/admin") || uri.startsWith("/api/")
                || uri.startsWith("/assets/") || uri.startsWith("/uploads/")
                || uri.startsWith("/favicon") || uri.startsWith("/apple-touch-icon");
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
        html = LEGACY_ASSET_STYLES.matcher(html).replaceAll("");
        html = LEGACY_VISUAL_SCRIPTS.matcher(html).replaceAll("");

        if (!html.contains("data-public-ui=\"nova\"")) {
            int htmlStart = html.toLowerCase(Locale.ROOT).indexOf("<html");
            if (htmlStart >= 0) {
                int tagEnd = html.indexOf('>', htmlStart);
                if (tagEnd > htmlStart) {
                    String tag = html.substring(htmlStart, tagEnd);
                    if (!tag.contains("data-public-ui=")) {
                        html = html.substring(0, tagEnd) + " data-public-ui=\"nova\"" + html.substring(tagEnd);
                    }
                }
            }
        }

        int headEnd = html.toLowerCase(Locale.ROOT).indexOf("</head>");
        if (headEnd >= 0) {
            StringBuilder missing = new StringBuilder();
            if (!html.contains(MARKER)) missing.append(MARKER);
            if (!html.contains("/assets/nova-ui.css")) missing.append(NOVA_CORE);
            if (!html.contains("/assets/nova-pages.css")) missing.append(NOVA_PAGES);
            if (!html.contains("/assets/nova-responsive.css")) missing.append(NOVA_RESPONSIVE);
            if (!html.contains("/assets/nova-shell.js")) missing.append(NOVA_SHELL);
            if (!missing.isEmpty()) html = html.substring(0, headEnd) + missing + html.substring(headEnd);
        }

        byte[] output = html.getBytes(charset);
        wrapper.resetBuffer();
        wrapper.getOutputStream().write(output);
        wrapper.setContentLength(output.length);
        wrapper.copyBodyToResponse();
    }
}
