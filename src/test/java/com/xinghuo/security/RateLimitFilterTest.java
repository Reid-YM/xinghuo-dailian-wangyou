package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void quoteAndQuoteUploadKeepTwelveRequestsPerMinute() throws Exception {
        assertThreshold("/api/quote", 12);
        assertThreshold("/api/uploads/quote", 12);
    }

    @Test
    void trackHasIndependentOneHundredTwentyRequestBucket() throws Exception {
        assertThreshold("/api/track", 120);
    }

    @Test
    void recordNumbersShareOneNormalizedThirtyRequestBucket() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < 30; i++) {
            String businessNo = i % 2 == 0 ? "XHABCDEFGHIJ" : "XHKLMNOPQRST";
            MockHttpServletResponse response = invoke(
                    filter, "/api/records/" + businessNo, "203.0.113.10", passed);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse blocked = invoke(
                filter, "/api/records/XHABCDEFGHIJ", "203.0.113.10", passed);
        assertEquals(429, blocked.getStatus());
        assertEquals(30, passed.get());
        assertTrue(blocked.getContentAsString().contains("RATE_LIMITED"));
    }

    @Test
    void differentTrustedClientIpsDoNotShareNginxRemoteAddressBucket() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < 30; i++) {
            MockHttpServletResponse response = invoke(
                    filter,
                    "/api/records/XHABCDEFGHIJ",
                    "127.0.0.1",
                    "203.0.113.21",
                    passed);
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse otherClient = invoke(
                filter,
                "/api/records/XHABCDEFGHIJ",
                "127.0.0.1",
                "203.0.113.22",
                passed);
        assertEquals(200, otherClient.getStatus());
        assertEquals(31, passed.get());
    }

    @Test
    void unrelatedPublicEndpointsRemainOutsideThisFilter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < 80; i++) {
            MockHttpServletResponse response = invoke(
                    filter, "/api/services", "203.0.113.11", passed);
            assertEquals(200, response.getStatus());
        }
        assertEquals(80, passed.get());
    }

    private void assertThreshold(String path, int allowed) throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < allowed; i++) {
            MockHttpServletResponse response = invoke(filter, path, "203.0.113.12", passed);
            assertEquals(200, response.getStatus());
        }
        MockHttpServletResponse blocked = invoke(filter, path, "203.0.113.12", passed);
        assertEquals(429, blocked.getStatus());
        assertEquals(allowed, passed.get());
        assertTrue(blocked.getContentAsString().contains("请求过于频繁"));
    }

    private MockHttpServletResponse invoke(
            RateLimitFilter filter,
            String path,
            String remoteAddress,
            AtomicInteger passed) throws Exception {
        return invoke(filter, path, remoteAddress, null, passed);
    }

    private MockHttpServletResponse invoke(
            RateLimitFilter filter,
            String path,
            String remoteAddress,
            String realIp,
            AtomicInteger passed) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(remoteAddress);
        if (realIp != null) {
            request.addHeader("X-Real-IP", realIp);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, (req, res) -> passed.incrementAndGet());
        return response;
    }
}
