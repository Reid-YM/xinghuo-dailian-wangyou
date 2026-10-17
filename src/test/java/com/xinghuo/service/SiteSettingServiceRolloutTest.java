package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SiteSettingServiceRolloutTest {

    @Test
    void serviceEntryDefaultsToLegacyAndUnknownPersistedValueFailsClosed() {
        SiteSettingRepository emptyRepo = mock(SiteSettingRepository.class);
        when(emptyRepo.findAll()).thenReturn(List.of());
        assertEquals("legacy", new SiteSettingService(emptyRepo).publicSettings().get("servicesEntryMode"));

        SiteSettingRepository invalidRepo = mock(SiteSettingRepository.class);
        when(invalidRepo.findAll()).thenReturn(List.of(setting("servicesEntryMode", "https://example.invalid")));
        assertEquals("legacy", new SiteSettingService(invalidRepo).publicSettings().get("servicesEntryMode"));
    }

    @Test
    void nextModeIsAllowListedAndPersistedAsNormalizedValue() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        when(repo.findById("servicesEntryMode")).thenReturn(Optional.empty());
        when(repo.save(any(SiteSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findAll()).thenReturn(List.of(setting("servicesEntryMode", "next")));
        SiteSettingService service = new SiteSettingService(repo);

        Map<String, String> result = service.updateGlobalUiSettings(Map.of("servicesEntryMode", "  NEXT  "));

        ArgumentCaptor<SiteSetting> saved = ArgumentCaptor.forClass(SiteSetting.class);
        verify(repo).save(saved.capture());
        assertEquals("servicesEntryMode", saved.getValue().getSettingKey());
        assertEquals("next", saved.getValue().getSettingValue());
        assertEquals("next", result.get("servicesEntryMode"));
    }

    @Test
    void arbitraryServiceEntryTargetIsRejectedBeforePersistence() {
        SiteSettingRepository repo = mock(SiteSettingRepository.class);
        SiteSettingService service = new SiteSettingService(repo);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.updateGlobalUiSettings(Map.of("servicesEntryMode", "/evil")));

        assertEquals("服务入口目标仅支持 legacy 或 next", error.getMessage());
        verify(repo, never()).save(any(SiteSetting.class));
    }

    private static SiteSetting setting(String key, String value) {
        return SiteSetting.builder().settingKey(key).settingValue(value).build();
    }
}
