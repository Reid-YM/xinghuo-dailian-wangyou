package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.model.SiteSetting;
import com.xinghuo.repository.SiteSettingRepository;
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
                new SiteSetting("homeHeroVisible", "false", Instant.now()),
                new SiteSetting("themeAccentColor", "#112233", Instant.now()),
                new SiteSetting("unrelatedKey", "ignored", Instant.now())
        ));

        Map<String, String> values = service.settings();

        assertEquals("false", values.get("homeHeroVisible"));
        assertEquals("#112233", values.get("themeAccentColor"));
        assertEquals("#0b0d10", values.get("themeBackgroundColor"));
    }

    @Test
    void rejectsExecutableOrProtocolRelativeLinks() {
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("homePrimaryCtaHref", "javascript:alert(1)")));
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("homePrimaryCtaHref", "//evil.example/path")));
    }

    @Test
    void rejectsOutOfRangeThemeValues() {
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("themeFontScale", "200")));
        assertThrows(BadRequestException.class,
                () -> service.update(Map.of("themeAccentColor", "yellow")));
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
}
