package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    @Test
    void validIncomingRequestIdIsTrimmedAndReused() throws Exception {
        RouteHealthMonitor monitor = mock(RouteHealthMonitor.class);
        RequestIdFilter filter = new RequestIdFilter(monitor);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services");
        request.addHeader(RequestIdFilter.HEADER, "  client-request-42  ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {});

        assertEquals("client-request-42", request.getAttribute(RequestIdFilter.ATTRIBUTE));
        assertEquals("client-request-42", response.getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void invalidIncomingRequestIdIsReplacedWithTwelveCharacterId() throws Exception {
        RouteHealthMonitor monitor = mock(RouteHealthMonitor.class);
        RequestIdFilter filter = new RequestIdFilter(monitor);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/services");
        String oversized = "x".repeat(65);
        request.addHeader(RequestIdFilter.HEADER, oversized);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {});

        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        assertEquals(12, requestId.length());
        assertNotEquals(oversized, requestId);
        assertEquals(requestId, response.getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void normalNotFoundResponseIsRecorded() throws Exception {
        RouteHealthMonitor monitor = mock(RouteHealthMonitor.class);
        RequestIdFilter filter = new RequestIdFilter(monitor);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(404));

        verify(monitor).recordNotFound("/missing");
    }

    @Test
    void exceptionalNotFoundResponseIsStillRecordedAndExceptionPropagates() {
        RouteHealthMonitor monitor = mock(RouteHealthMonitor.class);
        RequestIdFilter filter = new RequestIdFilter(monitor);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing-during-error");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ServletException thrown = assertThrows(ServletException.class, () ->
                filter.doFilterInternal(request, response, (req, res) -> {
                    ((HttpServletResponse) res).setStatus(404);
                    throw new ServletException("boom");
                }));

        assertTrue(thrown.getMessage().contains("boom"));
        verify(monitor).recordNotFound("/missing-during-error");
    }
}
