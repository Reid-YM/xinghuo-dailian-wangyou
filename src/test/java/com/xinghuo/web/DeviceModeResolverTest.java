package com.xinghuo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xinghuo.web.DeviceModeResolver.DeviceMode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class DeviceModeResolverTest {
    private final DeviceModeResolver resolver = new DeviceModeResolver();

    @Test
    void desktopChromeUsesDesktopView() {
        MockHttpServletRequest request = request("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/140 Safari/537.36");
        assertEquals(DeviceMode.DESKTOP, resolver.resolve(request));
    }

    @Test
    void iphoneUsesMobileView() {
        MockHttpServletRequest request = request("Mozilla/5.0 (iPhone; CPU iPhone OS 18_6 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1");
        assertEquals(DeviceMode.MOBILE, resolver.resolve(request));
    }

    @Test
    void androidPhoneUsesMobileView() {
        MockHttpServletRequest request = request("Mozilla/5.0 (Linux; Android 16; Pixel 10) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36");
        assertEquals(DeviceMode.MOBILE, resolver.resolve(request));
    }

    @Test
    void ipadStaysOnDesktopView() {
        MockHttpServletRequest request = request("Mozilla/5.0 (iPad; CPU OS 18_6 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1");
        assertEquals(DeviceMode.DESKTOP, resolver.resolve(request));
    }

    @Test
    void clientHintCanSelectMobileWithoutUaHeuristics() {
        MockHttpServletRequest request = request("Mozilla/5.0 AppleWebKit/537.36 Chrome/140 Safari/537.36");
        request.addHeader("Sec-CH-UA-Mobile", "?1");
        assertEquals(DeviceMode.MOBILE, resolver.resolve(request));
    }

    private MockHttpServletRequest request(String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", userAgent);
        return request;
    }
}
