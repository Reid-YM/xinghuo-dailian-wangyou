package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import com.xinghuo.web.DeviceModeResolver.DeviceMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FrontendControlServiceTest {
    @Mock
    private SiteSettingRepository repo;

    private FrontendControlService service;

    @BeforeEach
    void setUp() {
        service = new FrontendControlService(repo);
    }

    @Test
    void settingsMergeDefaultsWithPersistedOverrides() {
        when(repo.findAll()).thenReturn(List.of(
                setting("homeHeroVisible", "false"),
                setting("themeAccentColor", "#112233"),
                setting("unrelatedKey", "ignored")
        ));

        Map<String, String> values = service.settings();

        assertEquals("false", values.get("homeHeroVisible"));
        assertEquals("#112233", values.get("themeAccentColor"));
        assertEquals("#0b0d10", values.get("themeBackgroundColor"));
        assertEquals("0", values.get("homeHeroHeight"));
        assertEquals("0", values.get("homeCardScale"));
        assertEquals("询价", values.get("mobileDockQuoteLabel"));
    }

    @Test
    void deviceScopeOverridesGlobalWithoutAffectingOtherDevice() {
        when(repo.findAll()).thenReturn(List.of(
                setting("homeHeroVisible", "true"),
                setting("frontend.mobile.homeHeroVisible", "false"),
                setting("frontend.mobile.themeFontScale", "108"),
                setting("frontend.mobile.homeHeroHeight", "420")
        ));

        Map<String, String> mobile = service.effectiveSettings(DeviceMode.MOBILE);
        Map<String, String> desktop = service.effectiveSettings(DeviceMode.DESKTOP);

        assertEquals("false", mobile.get("homeHeroVisible"));
        assertEquals("108", mobile.get("themeFontScale"));
        assertEquals("420", mobile.get("homeHeroHeight"));
        assertEquals("true", desktop.get("homeHeroVisible"));
        assertEquals("100", desktop.get("themeFontScale"));
        assertEquals("0", desktop.get("homeHeroHeight"));
    }

    @Test
    void rejectsExecutableOrProtocolRelativeLinks() {
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("homePrimaryCtaHref", "javascript:alert(1)")));
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("homePrimaryCtaHref", "//evil.example/path")));
    }

    @Test
    void rejectsOutOfRangeThemeAndLayoutValues() {
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("themeFontScale", "200")));
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("themeAccentColor", "yellow")));
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("homeHeroHeight", "901")));
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("homeCardScale", "50")));
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("homeCardScale", "141")));
    }

    @Test
    void rejectsUnknownOrDuplicateHomeModules() {
        assertThrows(BadRequestException.class,
                () -> service.update("desktop", Map.of("homeModuleOrder", "hero,script,faq")));
        assertThrows(BadRequestException.class,
                () -> service.update("mobile", Map.of("homeModuleOrder", "hero,quote,hero")));
    }

    @Test
    void persistsValidatedSetting() {
        when(repo.findById("themeRadius")).thenReturn(Optional.empty());
        when(repo.save(any(SiteSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findAll()).thenReturn(List.of());

        service.update(Map.of("themeRadius", "22"));

        ArgumentCaptor<SiteSetting> captor = ArgumentCaptor.forClass(SiteSetting.class);
        verify(repo).save(captor.capture());
        assertEquals("themeRadius", captor.getValue().getSettingKey());
        assertEquals("22", captor.getValue().getSettingValue());
    }

    @Test
    void persistsMobileOverrideUnderNamespacedKey() {
        when(repo.findById("frontend.mobile.mobileDockVisible")).thenReturn(Optional.empty());
        when(repo.save(any(SiteSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findAll()).thenReturn(List.of());

        service.update("mobile", Map.of("mobileDockVisible", "false"));

        ArgumentCaptor<SiteSetting> captor = ArgumentCaptor.forClass(SiteSetting.class);
        verify(repo).save(captor.capture());
        assertEquals("frontend.mobile.mobileDockVisible", captor.getValue().getSettingKey());
        assertEquals("false", captor.getValue().getSettingValue());
    }

    @Test
    void persistsValidatedDesktopModuleOrder() {
        when(repo.findById("frontend.desktop.homeModuleOrder")).thenReturn(Optional.empty());
        when(repo.save(any(SiteSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findAll()).thenReturn(List.of());

        service.update("desktop", Map.of("homeModuleOrder", " hero,command,services,faq "));

        ArgumentCaptor<SiteSetting> captor = ArgumentCaptor.forClass(SiteSetting.class);
        verify(repo).save(captor.capture());
        assertEquals("frontend.desktop.homeModuleOrder", captor.getValue().getSettingKey());
        assertEquals("hero,command,services,faq", captor.getValue().getSettingValue());
    }

    @Test
    void rejectsUnknownScope() {
        assertThrows(BadRequestException.class,
                () -> service.settings("tablet"));
    }

    private SiteSetting setting(String key, String value) {
        return SiteSetting.builder().settingKey(key).settingValue(value).updatedAt(Instant.now()).build();
    }
}
