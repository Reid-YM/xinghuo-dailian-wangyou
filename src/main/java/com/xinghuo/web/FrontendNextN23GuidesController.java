package com.xinghuo.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Explicit Frontend Next guide-center preview route. */
@Controller
public class FrontendNextN23GuidesController {
    private final FrontendNextPreviewController preview;

    public FrontendNextN23GuidesController(FrontendNextPreviewController preview) {
        this.preview = preview;
    }

    @GetMapping("/next/guides")
    public ResponseEntity<String> guides(HttpServletRequest request) throws IOException {
        return preview.frontendNext(request);
    }
}
