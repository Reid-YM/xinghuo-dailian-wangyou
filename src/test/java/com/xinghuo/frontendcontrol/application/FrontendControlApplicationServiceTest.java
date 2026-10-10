package com.xinghuo.frontendcontrol.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FrontendControlApplicationServiceTest {
    private FrontendControlPort port;
    private FrontendControlApplicationService service;

    @BeforeEach
    void setUp() {
        port = mock(FrontendControlPort.class);
        service = new FrontendControlApplicationService(port);
        when(port.loadAll()).thenReturn(Map.of());
    }

    @Test
    void settingsMergeDefaultsWithPersistedOverrides() {
        when(port.loadAll()).thenReturn(Map.of(
                "homeHeroVisible", "false",
                "themeAccentColor", "#112233",
                "unrelatedKey", "ignored"));

        Map<String, String> values = service.settings();

        assertEquals("false", values.get("homeHeroVisible"));
        assertEquals("#112233", values.get("themeAccentColor"));
        assertEquals("#0b0d10", values.get("themeBackgroundColor"));
    }

    @Test
    void rejectsExecutableOrProtocolRelativeLinks() {
        assertThrows(BadRequestException.class,
                () -> service.update(new FrontendControlUpdateCommand(
                        Map.of("homePrimaryCtaHref", "javascript:alert(1)"))));
        assertThrows(BadRequestException.class,
                () -> service.update(new FrontendControlUpdateCommand(
                        Map.of("homePrimaryCtaHref", "//evil.example/path"))));
    }

    @Test
    void rejectsOutOfRangeThemeValues() {
        assertThrows(BadRequestException.class,
                () -> service.update(new FrontendControlUpdateCommand(
                        Map.of("themeFontScale", "200"))));
        assertThrows(BadRequestException.class,
                () -> service.update(new FrontendControlUpdateCommand(
                        Map.of("themeAccentColor", "yellow"))));
    }

    @Test
    void persistsValidatedSetting() {
        service.update(new FrontendControlUpdateCommand(Map.of("themeRadius", "22")));
        verify(port).put("themeRadius", "22");
    }
}
