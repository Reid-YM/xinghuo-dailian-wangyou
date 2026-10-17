package com.xinghuo.web;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Frontend Next legal previews. The legal copy itself remains owned by the formal legacy pages. */
@Controller
public class FrontendNextN28LegalController {
    private final FrontendNextPreviewController preview;

    public FrontendNextN28LegalController(FrontendNextPreviewController preview) {
        this.preview = preview;
    }

    @GetMapping({"/next/rules", "/next/privacy"})
    public ResponseEntity<String> legal(HttpServletRequest request) throws IOException {
        return preview.frontendNext(request);
    }
}
