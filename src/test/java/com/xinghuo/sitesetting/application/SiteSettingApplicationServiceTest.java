package com.xinghuo.sitesetting.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.exception.BadRequestException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SiteSettingApplicationServiceTest {
    private SiteSettingPort port;
    private SiteSettingApplicationService service;

    @BeforeEach
    void setUp() {
        port = mock(SiteSettingPort.class);
        service = new SiteSettingApplicationService(port);
        when(port.loadAll()).thenReturn(Map.of());
    }

    @Test
    void publicSettingsExposeOnlyWhitelistAndNormalizeLegacyAccountLabels() {
        when(port.loadAll()).thenReturn(Map.of(
                "brandName", "星火",
                "navAccountLabel", "登录",
                "footerConsoleLabel", "用户控制台",
                "privateInternalKey", "should-not-leak"));

        Map<String, String> values = service.publicSettings();

        assertEquals("星火", values.get("brandName"));
        assertEquals("个人中心", values.get("navAccountLabel"));
        assertEquals("个人中心", values.get("footerConsoleLabel"));
        assertFalse(values.containsKey("privateInternalKey"));
    }

    @Test
    void invalidGlobalUiBatchDoesNotPersistAnything() {
        assertThrows(BadRequestException.class,
                () -> service.updateGlobalUiSettings(new GlobalUiSettingsUpdateCommand(
                        Map.of("navServicesVisible", "not-a-boolean"))));

        verify(port, never()).putAll(anyMap());
    }

    @Test
    void globalUiBatchIsNormalizedAndPersistedOnce() {
        LinkedHashMap<String, String> input = new LinkedHashMap<>();
        input.put("navServicesVisible", "FALSE");
        input.put("navServicesLabel", "   ");

        service.updateGlobalUiSettings(new GlobalUiSettingsUpdateCommand(input));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(port).putAll(captor.capture());
        assertEquals("false", captor.getValue().get("navServicesVisible"));
        assertEquals("服务", captor.getValue().get("navServicesLabel"));
    }

    @Test
    void putKeepsPublicWhitelistBoundary() {
        service.put("brandName", "星火网游");
        service.put("privateInternalKey", "blocked");

        verify(port).put("brandName", "星火网游");
        verify(port, never()).put("privateInternalKey", "blocked");
    }
}
