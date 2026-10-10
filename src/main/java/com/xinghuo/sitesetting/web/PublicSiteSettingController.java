package com.xinghuo.sitesetting.web;

import com.xinghuo.sitesetting.application.SiteSettingApplicationService;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/public/site-settings"})
public class PublicSiteSettingController {
    private final SiteSettingApplicationService siteSettings;

    @GetMapping
    Map<String, String> siteSettings() {
        return this.siteSettings.publicSettings();
    }

    @Generated
    public PublicSiteSettingController(SiteSettingApplicationService siteSettings) {
        this.siteSettings = siteSettings;
    }
}
