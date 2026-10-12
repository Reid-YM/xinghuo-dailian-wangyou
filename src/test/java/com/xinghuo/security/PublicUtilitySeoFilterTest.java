package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PublicUtilitySeoFilterTest {

    private final PublicUtilitySeoFilter filter = new PublicUtilitySeoFilter();

    @Test
    void legacyOrderListPermanentlyRedirectsToRecords() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/order");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(301, response.getStatus());
        assertEquals("/records", response.getHeader("Location"));
        assertEquals("noindex, follow", response.getHeader("X-Robots-Tag"));
    }

    @Test
    void legacyOrderDetailPermanentlyRedirectsToRecordDetail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/order/XH7KM4Q2AZ8P");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(301, response.getStatus());
        assertEquals("/record/XH7KM4Q2AZ8P", response.getHeader("Location"));
        assertEquals("noindex, follow", response.getHeader("X-Robots-Tag"));
    }

    @Test
    void currentRecordUtilitiesRemainReachableButNoindex() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/records");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertEquals("noindex, follow", response.getHeader("X-Robots-Tag"));
    }
}
