package com.xinghuo.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    @Test
    void preservesSafeCallerRequestIdAndCleansMdc() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/services");
        request.addHeader(RequestCorrelationFilter.HEADER, "client-request-1234");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                seenInsideChain.set(MDC.get(RequestCorrelationFilter.MDC_KEY)));

        assertEquals("client-request-1234", response.getHeader(RequestCorrelationFilter.HEADER));
        assertEquals("client-request-1234", seenInsideChain.get());
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeCallerRequestId() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/quote");
        request.addHeader(RequestCorrelationFilter.HEADER, "bad id\r\ninjected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        String generated = response.getHeader(RequestCorrelationFilter.HEADER);
        assertNotNull(generated);
        assertFalse(generated.contains("bad id"));
        assertFalse(generated.contains("\r"));
        assertFalse(generated.contains("\n"));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }
}
