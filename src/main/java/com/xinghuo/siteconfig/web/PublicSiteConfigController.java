package com.xinghuo.siteconfig.web;

import com.xinghuo.siteconfig.application.SiteConfigPublicApplicationService;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/site-config"})
public class PublicSiteConfigController {
    private final SiteConfigPublicApplicationService siteConfig;

    @GetMapping
    SiteConfigResponse config() {
        return SiteConfigWebMapper.toResponse(this.siteConfig.get());
    }

    @Generated
    public PublicSiteConfigController(SiteConfigPublicApplicationService siteConfig) {
        this.siteConfig = siteConfig;
    }
}
