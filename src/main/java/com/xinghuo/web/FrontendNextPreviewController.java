package com.xinghuo.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Frontend Next parallel entry.
 *
 * <p>The legacy public frontend remains untouched. New UI is exposed under
 * /next so it can be integrated and replaced page-by-page with a clear rollback path.</p>
 */
@Controller
public class FrontendNextPreviewController {
    private static final String N2_RUNTIME = "<script src=\"/next/n2.js\" defer></script>";
    private static final String N3_RUNTIME = "<script src=\"/next/n3.js\" defer></script>";
    private static final String N4_RUNTIME = "<script src=\"/next/n4.js\" defer></script>";
    private static final String N4_MINI_RUNTIME = "<script src=\"/next/n4-mini.js\" defer></script>";
    private final ClassPathResource index = new ClassPathResource("static/next/index.html");

    @GetMapping({
            "/next",
            "/next/",
            "/next/index.html",
            "/next/home",
            "/next/services",
            "/next/quote",
            "/next/account"
    })
    public ResponseEntity<String> frontendNext() throws IOException {
        String html;
        try (var input = index.getInputStream()) {
            html = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        }
        if (!html.contains(N2_RUNTIME)) {
            html = html.replace("</body>", N2_RUNTIME + "</body>");
        }
        if (!html.contains(N3_RUNTIME)) {
            html = html.replace("</body>", N3_RUNTIME + "</body>");
        }
        if (!html.contains(N4_RUNTIME)) {
            html = html.replace("</body>", N4_RUNTIME + "</body>");
        }
        if (!html.contains(N4_MINI_RUNTIME)) {
            html = html.replace("</body>", N4_MINI_RUNTIME + "</body>");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .body(html);
    }
}
