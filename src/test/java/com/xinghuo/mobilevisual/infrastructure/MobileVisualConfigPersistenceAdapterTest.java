package com.xinghuo.mobilevisual.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.mobilevisual.application.UpdateMobileVisualConfigCommand;
import com.xinghuo.model.SiteConfig;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteConfigRepository;
import com.xinghuo.repository.SiteSettingRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MobileVisualConfigPersistenceAdapterTest {

    @Test
    void missingMobileValuesInheritDesktopThenPersistIndependently() {
        SiteSettingRepository settings = mock(SiteSettingRepository.class);
        SiteConfigRepository siteConfigs = mock(SiteConfigRepository.class);
        Map<String, SiteSetting> stored = new HashMap<>();

        when(settings.findById(any(String.class)))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(settings.save(any(SiteSetting.class))).thenAnswer(invocation -> {
            SiteSetting value = invocation.getArgument(0);
            stored.put(value.getSettingKey(), value);
            return value;
        });

        SiteConfig desktop = new SiteConfig();
        desktop.setHeroBackgroundOpacity(82);
        desktop.setHeroBackgroundBlur(4);
        when(siteConfigs.findById(1L)).thenReturn(Optional.of(desktop));

        MobileVisualConfigPersistenceAdapter adapter =
                new MobileVisualConfigPersistenceAdapter(settings, siteConfigs);

        var inherited = adapter.get();
        assertEquals(82, inherited.heroBackgroundMobileOpacity());
        assertEquals(4, inherited.heroBackgroundMobileBlur());

        var saved = adapter.update(new UpdateMobileVisualConfigCommand(47, 9));
        assertEquals(47, saved.heroBackgroundMobileOpacity());
        assertEquals(9, saved.heroBackgroundMobileBlur());
        assertEquals("47", stored.get("heroBackgroundMobileOpacity").getSettingValue());
        assertEquals("9", stored.get("heroBackgroundMobileBlur").getSettingValue());

        desktop.setHeroBackgroundOpacity(15);
        desktop.setHeroBackgroundBlur(1);
        var independent = adapter.get();
        assertEquals(47, independent.heroBackgroundMobileOpacity());
        assertEquals(9, independent.heroBackgroundMobileBlur());
    }

    @Test
    void corruptStoredValuesFailOpenToCurrentDesktopValues() {
        SiteSettingRepository settings = mock(SiteSettingRepository.class);
        SiteConfigRepository siteConfigs = mock(SiteConfigRepository.class);
        SiteConfig desktop = new SiteConfig();
        desktop.setHeroBackgroundOpacity(76);
        desktop.setHeroBackgroundBlur(6);
        when(siteConfigs.findById(1L)).thenReturn(Optional.of(desktop));
        when(settings.findById("heroBackgroundMobileOpacity"))
                .thenReturn(Optional.of(new SiteSetting("heroBackgroundMobileOpacity", "bad", Instant.now())));
        when(settings.findById("heroBackgroundMobileBlur"))
                .thenReturn(Optional.of(new SiteSetting("heroBackgroundMobileBlur", "999", Instant.now())));

        MobileVisualConfigPersistenceAdapter adapter =
                new MobileVisualConfigPersistenceAdapter(settings, siteConfigs);
        var result = adapter.get();
        assertEquals(76, result.heroBackgroundMobileOpacity());
        assertEquals(24, result.heroBackgroundMobileBlur());
    }
}
