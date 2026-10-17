package com.xinghuo.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Explicit Frontend Next mastery-tool preview route. */
@Controller
public class FrontendNextN24MasteryController {
    private final FrontendNextPreviewController preview;

    public FrontendNextN24MasteryController(FrontendNextPreviewController preview) {
        this.preview = preview;
    }

    @GetMapping("/next/mastery")
    public ResponseEntity<String> mastery(HttpServletRequest request) throws IOException {
        return preview.frontendNext(request);
    }
}
