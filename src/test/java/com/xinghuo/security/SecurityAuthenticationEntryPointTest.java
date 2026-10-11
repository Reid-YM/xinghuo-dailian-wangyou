package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

class SecurityAuthenticationEntryPointTest {

    @Test
    void adminPageRedirectsToLoginForAnonymousRequest() throws Exception {
        AuthenticationEntryPoint entryPoint = SecurityConfig.authenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        request.setServletPath("/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("anonymous"));

        assertEquals(302, response.getStatus());
        assertTrue(response.getRedirectedUrl() != null && response.getRedirectedUrl().endsWith("/admin/login"));
    }

    @Test
    void adminApiKeepsUnauthorizedStatusInsteadOfHtmlRedirect() throws Exception {
        AuthenticationEntryPoint entryPoint = SecurityConfig.authenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/site-config");
        request.setServletPath("/api/admin/site-config");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("anonymous"));

        assertEquals(401, response.getStatus());
        assertNull(response.getRedirectedUrl());
    }
}
