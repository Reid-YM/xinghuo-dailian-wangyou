package com.xinghuo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Frontend Next parallel preview entry.
 *
 * <p>The legacy public frontend remains untouched. New UI is exposed under
 * /next so it can be integrated and replaced page-by-page with a clear rollback path.</p>
 */
@Controller
public class FrontendNextPreviewController {

    @GetMapping({
            "/next",
            "/next/",
            "/next/home",
            "/next/services",
            "/next/quote",
            "/next/account"
    })
    public String frontendNext() {
        return "forward:/next/index.html";
    }
}
