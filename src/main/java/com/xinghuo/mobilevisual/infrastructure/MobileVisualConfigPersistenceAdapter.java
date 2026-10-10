package com.xinghuo.mobilevisual.infrastructure;

import com.xinghuo.mobilevisual.application.MobileVisualConfigPort;
import com.xinghuo.mobilevisual.application.MobileVisualConfigView;
import com.xinghuo.mobilevisual.application.UpdateMobileVisualConfigCommand;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteConfigRepository;
import com.xinghuo.repository.SiteSettingRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MobileVisualConfigPersistenceAdapter implements MobileVisualConfigPort {
    static final String OPACITY_KEY = "heroBackgroundMobileOpacity";
    static final String BLUR_KEY = "heroBackgroundMobileBlur";

    private final SiteSettingRepository settings;
    private final SiteConfigRepository siteConfigs;

    public MobileVisualConfigPersistenceAdapter(SiteSettingRepository settings, SiteConfigRepository siteConfigs) {
        this.settings = settings;
        this.siteConfigs = siteConfigs;
    }

    @Override
    @Transactional(readOnly = true)
    public MobileVisualConfigView get() {
        SiteConfig desktop = siteConfigs.findById(1L).orElseThrow();
        int opacity = readInt(OPACITY_KEY, desktop.getHeroBackgroundOpacity(), 0, 100);
        int blur = readInt(BLUR_KEY, desktop.getHeroBackgroundBlur(), 0, 24);
        return new MobileVisualConfigView(opacity, blur);
    }

    @Override
    @Transactional
    public MobileVisualConfigView update(UpdateMobileVisualConfigCommand command) {
        int safeOpacity = clamp(command.heroBackgroundMobileOpacity(), 0, 100);
        int safeBlur = clamp(command.heroBackgroundMobileBlur(), 0, 24);
        write(OPACITY_KEY, safeOpacity);
        write(BLUR_KEY, safeBlur);
        return new MobileVisualConfigView(safeOpacity, safeBlur);
    }

    private int readInt(String key, int fallback, int min, int max) {
        String raw = settings.findById(key).map(SiteSetting::getSettingValue).orElse(null);
        if (raw == null || raw.isBlank()) {
            return clamp(fallback, min, max);
        }
        try {
            return clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return clamp(fallback, min, max);
        }
    }

    private void write(String key, int value) {
        SiteSetting setting = settings.findById(key)
                .orElseGet(() -> SiteSetting.builder().settingKey(key).build());
        setting.setSettingValue(String.valueOf(value));
        setting.setUpdatedAt(Instant.now());
        settings.save(setting);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
