package com.xinghuo.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Explicit Frontend Next submission-workbench preview route. */
@Controller
public class FrontendNextN25SubmissionController {
    private final FrontendNextPreviewController preview;

    public FrontendNextN25SubmissionController(FrontendNextPreviewController preview) {
        this.preview = preview;
    }

    @GetMapping("/next/account/submit")
    public ResponseEntity<String> submissionWorkbench(HttpServletRequest request) throws IOException {
        return preview.frontendNext(request);
    }
}
