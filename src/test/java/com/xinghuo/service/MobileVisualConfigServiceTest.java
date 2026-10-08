package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.model.SiteConfig;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MobileVisualConfigServiceTest {

    @Test
    void missingMobileValuesInheritDesktopThenPersistIndependently() {
        SiteSettingRepository repository = mock(SiteSettingRepository.class);
        SiteConfigService siteConfigService = mock(SiteConfigService.class);
        Map<String, SiteSetting> stored = new HashMap<>();

        when(repository.findById(any(String.class)))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(repository.save(any(SiteSetting.class))).thenAnswer(invocation -> {
            SiteSetting value = invocation.getArgument(0);
            stored.put(value.getSettingKey(), value);
            return value;
        });

        SiteConfig desktop = new SiteConfig();
        desktop.setHeroBackgroundOpacity(82);
        desktop.setHeroBackgroundBlur(4);
        when(siteConfigService.get()).thenReturn(desktop);

        MobileVisualConfigService service = new MobileVisualConfigService(repository, siteConfigService);

        var inherited = service.get();
        assertEquals(82, inherited.heroBackgroundMobileOpacity());
        assertEquals(4, inherited.heroBackgroundMobileBlur());

        var saved = service.update(47, 9);
        assertEquals(47, saved.heroBackgroundMobileOpacity());
        assertEquals(9, saved.heroBackgroundMobileBlur());
        assertEquals("47", stored.get("heroBackgroundMobileOpacity").getSettingValue());
        assertEquals("9", stored.get("heroBackgroundMobileBlur").getSettingValue());

        desktop.setHeroBackgroundOpacity(15);
        desktop.setHeroBackgroundBlur(1);
        var independent = service.get();
        assertEquals(47, independent.heroBackgroundMobileOpacity());
        assertEquals(9, independent.heroBackgroundMobileBlur());
    }

    @Test
    void corruptStoredValuesFailOpenToCurrentDesktopValues() {
        SiteSettingRepository repository = mock(SiteSettingRepository.class);
        SiteConfigService siteConfigService = mock(SiteConfigService.class);
        SiteConfig desktop = new SiteConfig();
        desktop.setHeroBackgroundOpacity(76);
        desktop.setHeroBackgroundBlur(6);
        when(siteConfigService.get()).thenReturn(desktop);
        when(repository.findById("heroBackgroundMobileOpacity"))
                .thenReturn(Optional.of(new SiteSetting("heroBackgroundMobileOpacity", "bad", Instant.now())));
        when(repository.findById("heroBackgroundMobileBlur"))
                .thenReturn(Optional.of(new SiteSetting("heroBackgroundMobileBlur", "999", Instant.now())));

        MobileVisualConfigService service = new MobileVisualConfigService(repository, siteConfigService);
        var result = service.get();
        assertEquals(76, result.heroBackgroundMobileOpacity());
        assertEquals(24, result.heroBackgroundMobileBlur());
    }
}
