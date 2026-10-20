package com.xinghuo.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Next legal pages and a read-only fragment of the single published legal source. */
@Controller
public class FrontendNextN28LegalController {
    private final FrontendNextPreviewController preview;

    public FrontendNextN28LegalController(FrontendNextPreviewController preview) {
        this.preview = preview;
    }

    @GetMapping("/api/public/legal/{document:rules|privacy|rules-en|privacy-en}")
    public String document(@org.springframework.web.bind.annotation.PathVariable String document,
                           jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Robots-Tag", "noindex,nofollow");
        // Return content only: no legacy navigation, scripts or duplicate legal copy.
        return document + " :: main";
    }

    @GetMapping({"/next/rules", "/next/privacy", "/next/rules-en", "/next/privacy-en"})
    public ResponseEntity<String> legal(HttpServletRequest request) throws IOException {
        return preview.frontendNext(request);
    }
}
