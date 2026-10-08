package com.xinghuo.service;

import com.xinghuo.model.SiteConfig;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores mobile-only visual tuning in the existing site_settings key/value table.
 * This intentionally avoids adding columns to site_config so mobile visual controls
 * can evolve without a database migration. Missing keys inherit the current desktop
 * values, preserving backward compatibility for existing installations.
 */
@Service
public class MobileVisualConfigService {
    static final String OPACITY_KEY = "heroBackgroundMobileOpacity";
    static final String BLUR_KEY = "heroBackgroundMobileBlur";

    private final SiteSettingRepository settings;
    private final SiteConfigService siteConfig;

    public MobileVisualConfigService(SiteSettingRepository settings, SiteConfigService siteConfig) {
        this.settings = settings;
        this.siteConfig = siteConfig;
    }

    @Transactional(readOnly = true)
    public MobileVisualConfig get() {
        SiteConfig desktop = siteConfig.get();
        int opacity = readInt(OPACITY_KEY, desktop.getHeroBackgroundOpacity(), 0, 100);
        int blur = readInt(BLUR_KEY, desktop.getHeroBackgroundBlur(), 0, 24);
        return new MobileVisualConfig(opacity, blur);
    }

    @Transactional
    public MobileVisualConfig update(int opacity, int blur) {
        int safeOpacity = clamp(opacity, 0, 100);
        int safeBlur = clamp(blur, 0, 24);
        write(OPACITY_KEY, safeOpacity);
        write(BLUR_KEY, safeBlur);
        return new MobileVisualConfig(safeOpacity, safeBlur);
    }

    private int readInt(String key, int fallback, int min, int max) {
        String raw = settings.findById(key).map(SiteSetting::getSettingValue).orElse(null);
        if (raw == null || raw.isBlank()) return clamp(fallback, min, max);
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

    public record MobileVisualConfig(int heroBackgroundMobileOpacity, int heroBackgroundMobileBlur) {
    }
}
