package com.xinghuo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.xinghuo.exception.NotFoundException;
import com.xinghuo.service.XingweiPaymentWorkspaceService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class XingweiPaymentOrderLookupControllerTest {
    @Test
    void missingOrderExplainsThatQuoteHasNotYetBecomeAnOrder() {
        XingweiPaymentWorkspaceService payments = mock(XingweiPaymentWorkspaceService.class);
        Authentication admin = new UsernamePasswordAuthenticationToken("staff", "", java.util.List.of());
        when(payments.order("XH-QUOTE-ONLY", admin)).thenThrow(new NotFoundException());

        Object result = new XingweiPaymentWorkspaceController(payments).order("XH-QUOTE-ONLY", admin);

        assertInstanceOf(ResponseEntity.class, result);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(404, response.getStatusCode().value());
        assertInstanceOf(Map.class, response.getBody());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("ORDER_NOT_FOUND", body.get("code"));
        assertTrue(((String) body.get("message")).contains("尚未接受正式报价"));
        verify(payments).order("XH-QUOTE-ONLY", admin);
    }

    @Test
    void accessDeniedIsNotDisguisedAsOrderNotFound() {
        XingweiPaymentWorkspaceService payments = mock(XingweiPaymentWorkspaceService.class);
        Authentication staff = new UsernamePasswordAuthenticationToken("staff", "", java.util.List.of());
        when(payments.order("B-FOREIGN", staff))
                .thenThrow(new AccessDeniedException("支付业务不在可见数据范围内"));

        assertThrows(AccessDeniedException.class,
                () -> new XingweiPaymentWorkspaceController(payments).order("B-FOREIGN", staff));
    }
}
