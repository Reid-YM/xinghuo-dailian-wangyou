package com.xinghuo.page.web;

import com.xinghuo.catalog.application.CatalogApplicationService;
import lombok.Generated;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class PublicPageModelSupport {
    private static final String DEFAULT_WECHAT = "AAC9878";
    private final CatalogApplicationService services;

    public void fill(Model model) {
        model.addAttribute("contactFallback", DEFAULT_WECHAT);
        model.addAttribute("homeServices", this.services.publicPageItems().stream().limit(6L).toList());
    }

    @Generated
    public PublicPageModelSupport(CatalogApplicationService services) {
        this.services = services;
    }
}
