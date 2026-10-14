package com.xinghuo.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

class WeChatPaymentPermissionTest {
    @Test
    void adminPaymentReadsAndWritesHaveSeparatePermissions() throws Exception {
        assertEquals("@xingweiPermissionGuard.has(authentication, 'payment:manage')",
                permission("prepare", String.class, WeChatPaymentController.QuoteAmount.class,
                        Authentication.class, HttpServletRequest.class));
        assertEquals("@xingweiPermissionGuard.has(authentication, 'payment:view')",
                permission("adminLatest", String.class, Authentication.class));
        assertEquals("@xingweiPermissionGuard.has(authentication, 'payment:manage')",
                permission("close", String.class, Authentication.class, HttpServletRequest.class));
    }

    private String permission(String method, Class<?>... types) throws Exception {
        return WeChatPaymentController.class.getMethod(method, types)
                .getAnnotation(PreAuthorize.class).value();
    }
}
