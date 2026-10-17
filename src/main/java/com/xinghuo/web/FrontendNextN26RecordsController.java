package com.xinghuo.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Explicit Frontend Next public records-center preview route. */
@Controller
public class FrontendNextN26RecordsController {
    private final FrontendNextPreviewController preview;

    public FrontendNextN26RecordsController(FrontendNextPreviewController preview) {
        this.preview = preview;
    }

    @GetMapping("/next/records")
    public ResponseEntity<String> records(HttpServletRequest request) throws IOException {
        return preview.frontendNext(request);
    }
}
