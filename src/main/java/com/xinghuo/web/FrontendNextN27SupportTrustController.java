package com.xinghuo.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Explicit Frontend Next support and trust preview routes. */
@Controller
public class FrontendNextN27SupportTrustController {
    private final FrontendNextPreviewController preview;

    public FrontendNextN27SupportTrustController(FrontendNextPreviewController preview) {
        this.preview = preview;
    }

    @GetMapping({"/next/contact", "/next/trust"})
    public ResponseEntity<String> supportTrust(HttpServletRequest request) throws IOException {
        return preview.frontendNext(request);
    }
}
