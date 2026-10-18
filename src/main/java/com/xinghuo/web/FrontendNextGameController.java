package com.xinghuo.web;

import com.xinghuo.service.TaxonomyService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Uses the same public visibility check as the original game page. */
@Controller
public class FrontendNextGameController {
    private final FrontendNextPreviewController preview;
    private final TaxonomyService taxonomy;

    public FrontendNextGameController(FrontendNextPreviewController preview, TaxonomyService taxonomy) {
        this.preview = preview;
        this.taxonomy = taxonomy;
    }

    @GetMapping("/next/game/{seoSlug}")
    public ResponseEntity<String> game(@PathVariable String seoSlug, HttpServletRequest request) throws IOException {
        request.setAttribute("frontendGame", taxonomy.publicGameBySeoSlug(seoSlug));
        return preview.frontendNext(request);
    }
}
