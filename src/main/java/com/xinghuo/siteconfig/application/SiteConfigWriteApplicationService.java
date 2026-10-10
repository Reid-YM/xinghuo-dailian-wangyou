package com.xinghuo.siteconfig.application;

import com.xinghuo.sitesetting.application.SiteSettingApplicationService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application boundary for SiteConfig mutations. */
@Service
public class SiteConfigWriteApplicationService {
    private final SiteConfigWritePort port;
    private final SiteConfigPublicApplicationService query;
    private final SiteSettingApplicationService siteSettings;

    public SiteConfigWriteApplicationService(
            SiteConfigWritePort port,
            SiteConfigPublicApplicationService query,
            SiteSettingApplicationService siteSettings) {
        this.port = port;
        this.query = query;
        this.siteSettings = siteSettings;
    }

    @Transactional
    public void updateSiteImage(String slot, String url) {
        port.updateSiteImage(slot, url);
    }

    @Transactional
    public void update(SiteConfigFullUpdateCommand command) {
        port.update(command);
    }

    @Transactional
    public void updateSection(String section, Map<String, Object> values) {
        port.updateSection(section, values);
        syncSiteSettings();
    }

    private void syncSiteSettings() {
        SiteConfigPublicSnapshot snapshot = query.get().snapshot();
        siteSettings.put("brandName", snapshot.brandName());
        siteSettings.put("legalEntityName", snapshot.legalEntityName());
        siteSettings.put("wechat", snapshot.wechatId());
        siteSettings.put("serviceHours", snapshot.serviceHours());
        siteSettings.put("siteTitleSuffix", snapshot.brandName());
    }
}
