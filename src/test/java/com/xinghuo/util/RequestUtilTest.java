package com.xinghuo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestUtilTest {

    @Test
    void prefersTrustedRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.10, 198.51.100.20");
        request.addHeader("X-Real-IP", "203.0.113.9");

        assertEquals("203.0.113.9", RequestUtil.clientIp(request));
    }

    @Test
    void usesLastForwardedForHopWhenRealIpIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.10, 203.0.113.8");

        assertEquals("203.0.113.8", RequestUtil.clientIp(request));
    }

    @Test
    void fallsBackToRemoteAddressWithoutProxyHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.44");

        assertEquals("192.0.2.44", RequestUtil.clientIp(request));
    }

    @Test
    void returnsUnknownForMissingRequest() {
        assertEquals("unknown", RequestUtil.clientIp(null));
    }
}
