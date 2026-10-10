package com.xinghuo.vnext.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/__preview"})
public class VNextPreviewController {
    @GetMapping({"/vnext", "/vnext/**"})
    String preview() {
        return "forward:/vnext/index.html";
    }

    @GetMapping(value="/vnext/assets/**", produces={"text/plain"})
    String previewAssets() {
        return "forward:/vnext/index.html";
    }
}
