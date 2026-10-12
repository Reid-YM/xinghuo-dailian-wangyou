package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestUtilsClientIpCharacterizationTest {

    @Test
    void prefersRealIpOverForwardedForAndRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.10, 203.0.113.20");
        request.addHeader("X-Real-IP", "203.0.113.9");

        assertEquals("203.0.113.9", RequestUtils.clientIp(request));
    }

    @Test
    void usesLastForwardedForHopWhenRealIpIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.10, 203.0.113.8");

        assertEquals("203.0.113.8", RequestUtils.clientIp(request));
    }

    @Test
    void fallsBackToRemoteAddressWithoutProxyHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.44");

        assertEquals("192.0.2.44", RequestUtils.clientIp(request));
    }

    @Test
    void nullRequestReturnsNull() {
        assertNull(RequestUtils.clientIp(null));
    }
}
