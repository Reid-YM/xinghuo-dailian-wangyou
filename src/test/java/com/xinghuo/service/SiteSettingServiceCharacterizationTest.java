package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SiteSettingServiceCharacterizationTest {

    @Test
    void publicSettingsKeepsCriticalRegistrationDefaultsAndNormalizesLegacyAccountLabels() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                setting("navAccountLabel", "登录"),
                setting("footerConsoleLabel", "用户控制台"),
                setting("internalOnlyKey", "secret")));
        SiteSettingService service = new SiteSettingService(repo);

        Map<String, String> settings = service.publicSettings();

        assertEquals("蜀ICP备2026047129号", settings.get("icpNumber"));
        assertEquals("川公网安备51162102000262号", settings.get("publicSecurityNumber"));
        assertEquals("个人中心", settings.get("navAccountLabel"));
        assertEquals("个人中心", settings.get("footerConsoleLabel"));
        assertFalse(settings.containsKey("internalOnlyKey"));
    }

    @Test
    void customAccountLabelsArePreservedInsteadOfBeingRewritten() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                setting("navAccountLabel", "会员中心"),
                setting("footerConsoleLabel", "我的工作台")));
        SiteSettingService service = new SiteSettingService(repo);

        Map<String, String> settings = service.publicSettings();

        assertEquals("会员中心", settings.get("navAccountLabel"));
        assertEquals("我的工作台", settings.get("footerConsoleLabel"));
    }

    @Test
    void invalidBooleanValueIsRejectedBeforeAnySettingIsPersisted() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        SiteSettingService service = new SiteSettingService(repo);

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> service.updateGlobalUiSettings(Map.of("navServicesVisible", "yes")));

        assertEquals("显示开关必须为 true 或 false：navServicesVisible", error.getMessage());
        verifyNoInteractions(repo);
    }

    @Test
    void blankLabelResetsToDefaultBeforeSave() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        when(repo.findById("navServicesLabel")).thenReturn(Optional.empty());
        when(repo.save(any(SiteSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findAll()).thenReturn(List.of());
        SiteSettingService service = new SiteSettingService(repo);

        Map<String, String> settings = service.updateGlobalUiSettings(Map.of("navServicesLabel", "   "));

        ArgumentCaptor<SiteSetting> saved = ArgumentCaptor.forClass(SiteSetting.class);
        verify(repo).save(saved.capture());
        assertEquals("navServicesLabel", saved.getValue().getSettingKey());
        assertEquals("服务", saved.getValue().getSettingValue());
        assertEquals("服务", settings.get("navServicesLabel"));
    }

    @Test
    void booleanValueIsTrimmedAndNormalizedToLowercase() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        when(repo.findById("navServicesVisible")).thenReturn(Optional.empty());
        when(repo.save(any(SiteSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findAll()).thenReturn(List.of());
        SiteSettingService service = new SiteSettingService(repo);

        service.updateGlobalUiSettings(Map.of("navServicesVisible", "  TRUE  "));

        ArgumentCaptor<SiteSetting> saved = ArgumentCaptor.forClass(SiteSetting.class);
        verify(repo).save(saved.capture());
        assertEquals("true", saved.getValue().getSettingValue());
    }

    @Test
    void putIgnoresNonPublicKeysAndBlankGetUsesFallback() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        SiteSettingService service = new SiteSettingService(repo);

        service.put("internalOnlyKey", "secret");
        verifyNoInteractions(repo);

        SiteSettingRepository readRepo = mock(SiteSettingRepository.class);
        when(readRepo.findById("brandName")).thenReturn(Optional.of(setting("brandName", "  ")));
        SiteSettingService readService = new SiteSettingService(readRepo);

        assertEquals("fallback-brand", readService.get("brandName", "fallback-brand"));
    }

    private static SiteSetting setting(String key, String value) {
        return SiteSetting.builder().settingKey(key).settingValue(value).build();
    }
}
