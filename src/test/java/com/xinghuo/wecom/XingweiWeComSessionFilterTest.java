package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class XingweiWeComSessionFilterTest {
    private final XingweiWeComBindingService bindings = mock(XingweiWeComBindingService.class);
    private final XingweiWeComProperties properties =
            new XingweiWeComProperties("ww-test", "server-only-secret", "1000003", true, true, false);
    private final XingweiWeComSessionFilter filter = new XingweiWeComSessionFilter(bindings, properties);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokedSsoSessionCannotUseAdminApi() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/admin/xingwei/work");
        request.getSession().setAttribute(XingweiWeComSsoController.SSO_SESSION_KEY,
                new XingweiWeComSsoController.SsoIdentity("operator", "staff_1", "OPERATOR"));
        var user = User.withUsername("operator").password("unused").roles("OPERATOR").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        when(bindings.activeSsoUser("operator", "staff_1", "OPERATOR")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(bindings).activeSsoUser("operator", "staff_1", "OPERATOR");
    }

    @Test
    void normalPasswordSessionKeepsExistingBehavior() throws Exception {
        var request = new MockHttpServletRequest("GET", "/admin/xingwei/work");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void activeSsoSessionCannotEnterLegacyAdmin() throws Exception {
        var request = new MockHttpServletRequest("GET", "/admin/orders");
        request.getSession().setAttribute(XingweiWeComSsoController.SSO_SESSION_KEY,
                new XingweiWeComSsoController.SsoIdentity("operator", "staff_1", "OPERATOR"));
        var user = User.withUsername("operator").password("unused").roles("OPERATOR").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        when(bindings.activeSsoUser("operator", "staff_1", "OPERATOR")).thenReturn(true);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        org.mockito.Mockito.verifyNoInteractions(chain);
    }

    @Test
    void activeSsoSessionCanUseXingweiWork() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/admin/xingwei/work");
        request.getSession().setAttribute(XingweiWeComSsoController.SSO_SESSION_KEY,
                new XingweiWeComSsoController.SsoIdentity("operator", "staff_1", "OPERATOR"));
        var user = User.withUsername("operator").password("unused").roles("OPERATOR").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        when(bindings.activeSsoUser("operator", "staff_1", "OPERATOR")).thenReturn(true);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void expiredSsoSessionCanRestartWeComLogin() throws Exception {
        var request = new MockHttpServletRequest("GET", "/admin/xingwei/wecom/login");
        request.getSession().setAttribute(XingweiWeComSsoController.SSO_SESSION_KEY,
                new XingweiWeComSsoController.SsoIdentity("operator", "staff_1", "OPERATOR"));
        var user = User.withUsername("operator").password("unused").roles("OPERATOR").build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        when(bindings.activeSsoUser("operator", "staff_1", "OPERATOR")).thenReturn(false);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }
}
