package com.xinghuo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.xinghuo.service.GuestQuoteFlowService;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

/** Cross-site forms cannot write against a guest's capability cookie. */
class GuestBusinessApiCsrfGuardTest {
    @Test
    void missingOrIncorrectHeaderCannotAcceptQuoteOrPreparePayment() {
        var service = mock(GuestQuoteFlowService.class);
        var controller = new GuestBusinessApiController(service);
        for (String header : new String[]{null, "XMLHttpRequest", "other"}) {
            var request = guestRequest("POST");
            if (header != null) request.addHeader("X-Requested-With", header);
            var accept = assertThrows(ResponseStatusException.class,
                    () -> controller.accept("B-100", request));
            var prepay = assertThrows(ResponseStatusException.class,
                    () -> controller.nativePayment("B-100", request));
            assertEquals(HttpStatus.FORBIDDEN.value(), accept.getStatusCode().value());
            assertEquals(HttpStatus.FORBIDDEN.value(), prepay.getStatusCode().value());
        }
        verifyNoInteractions(service);
    }

    @Test
    void sameOriginFetchStillPassesCapabilityToExistingBusinessValidation() {
        var service = mock(GuestQuoteFlowService.class);
        var controller = new GuestBusinessApiController(service);
        var request = guestRequest("POST");
        request.addHeader("X-Requested-With", "fetch");
        when(service.accept(eq("test-capability"), eq("B-100"), same(request)))
                .thenReturn(Map.of("ok", true));

        assertEquals(Boolean.TRUE, controller.accept("B-100", request).get("ok"));
        verify(service).accept("test-capability", "B-100", request);
    }

    @Test
    void readonlySnapshotDoesNotRequireWriteHeader() {
        var service = mock(GuestQuoteFlowService.class);
        var controller = new GuestBusinessApiController(service);
        var request = guestRequest("GET");
        when(service.snapshot("test-capability", "B-100"))
                .thenReturn(Map.of("state", "waiting"));

        assertEquals("waiting", controller.snapshot("B-100", request).get("state"));
        verify(service).snapshot("test-capability", "B-100");
    }

    private static MockHttpServletRequest guestRequest(String method) {
        var request = new MockHttpServletRequest(method, "/api/guest/business/B-100");
        request.setCookies(new Cookie(GuestQuoteFlowService.COOKIE_NAME, "test-capability"));
        return request;
    }
}
