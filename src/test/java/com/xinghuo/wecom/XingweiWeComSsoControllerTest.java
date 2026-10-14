package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.service.AuditService;
import java.net.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

class XingweiWeComSsoControllerTest {
    private final XingweiWeComDirectoryClient client = mock(XingweiWeComDirectoryClient.class);
    private final XingweiWeComBindingService bindings = mock(XingweiWeComBindingService.class);
    private final XingweiWeComPositionRoleService positionRoles = mock(XingweiWeComPositionRoleService.class);
    private final AuditService audits = mock(AuditService.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledEntryNeverCallsWeCom() {
        var controller = controller(false);
        assertThrows(ResponseStatusException.class,
                () -> controller.login(new MockHttpServletRequest(), new MockHttpServletResponse()));
        verify(client, never()).userIdForCode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void enabledEntryRequiresSecureSessionCookie() {
        var controller = new XingweiWeComSsoController(
                new XingweiWeComProperties("ww-test", "server-only-secret", "1000003", true, false),
                client, bindings, positionRoles, audits, "https://example.test");
        assertThrows(ResponseStatusException.class,
                () -> controller.login(new MockHttpServletRequest(), new MockHttpServletResponse()));
    }

    @Test
    void callbackRejectsWrongStateAndReplayBeforeCodeExchange() throws Exception {
        var controller = controller(true);
        var request = new MockHttpServletRequest();
        var start = new MockHttpServletResponse();
        controller.login(request, start);
        String authorize = start.getRedirectedUrl();
        assertTrue(authorize.startsWith("https://open.weixin.qq.com/connect/oauth2/authorize?"));
        assertTrue(authorize.contains("agentid=1000003"));
        assertTrue(authorize.contains("scope=snsapi_base"));
        assertTrue(authorize.contains("redirect_uri=https%3A%2F%2Fexample.test%2Fadmin%2Fxingwei%2Fwecom%2Fcallback"));
        String state = queryValue(authorize, "state");

        var invalid = new MockHttpServletResponse();
        controller.callback("one-time-code", "wrong", request, invalid);
        assertEquals(403, invalid.getStatus());
        var replay = new MockHttpServletResponse();
        controller.callback("one-time-code", state, request, replay);
        assertEquals(403, replay.getStatus());
        verify(client, never()).userIdForCode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void mappedMemberReceivesExistingSpringSecuritySession() throws Exception {
        var controller = controller(true);
        var request = new MockHttpServletRequest();
        var start = new MockHttpServletResponse();
        controller.login(request, start);
        String state = queryValue(start.getRedirectedUrl(), "state");
        when(client.userIdForCode("one-time-code")).thenReturn("staff_1");
        when(bindings.boundAdminUserId("staff_1")).thenReturn(2L);
        when(bindings.loginUser("staff_1"))
                .thenReturn(User.withUsername("operator").password("unused").roles("OPERATOR").build());

        var callback = new MockHttpServletResponse();
        controller.callback("one-time-code", state, request, callback);

        assertEquals("/admin/xingwei/work", callback.getRedirectedUrl());
        assertEquals("operator", SecurityContextHolder.getContext().getAuthentication().getName());
        SecurityContext saved = (SecurityContext) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        assertEquals("operator", saved.getAuthentication().getName());
        assertTrue(callback.getHeader("Cache-Control").contains("no-store"));
        verify(bindings).loginUser("staff_1");
        verify(client, never()).member("staff_1");
        var replay = new MockHttpServletResponse();
        controller.callback("one-time-code", state, request, replay);
        assertEquals(403, replay.getStatus());
    }

    @Test
    void firstVisitRegistersIdentityButWaitsForExistingRole() throws Exception {
        var controller = controller(true);
        var request = new MockHttpServletRequest();
        var start = new MockHttpServletResponse();
        controller.login(request, start);
        String state = queryValue(start.getRedirectedUrl(), "state");
        when(client.userIdForCode("one-time-code")).thenReturn("staff_1");
        when(bindings.boundAdminUserId("staff_1")).thenReturn(null);
        when(client.member("staff_1")).thenReturn(
                new XingweiWeComDirectoryClient.Member("staff_1", "员工甲", "未知职务"));
        when(bindings.registerPending("staff_1", "员工甲")).thenReturn(2L);
        when(bindings.loginUser("staff_1")).thenThrow(new IllegalArgumentException("未分配角色"));

        var callback = new MockHttpServletResponse();
        controller.callback("one-time-code", state, request, callback);

        assertEquals("/admin/xingwei/wecom/pending", callback.getRedirectedUrl());
        assertTrue(SecurityContextHolder.getContext().getAuthentication() == null);
        verify(bindings).registerPending("staff_1", "员工甲");
    }

    @Test
    void firstVisitWithMappedPositionReceivesWorkSession() throws Exception {
        var controller = controller(true);
        var request = new MockHttpServletRequest();
        var start = new MockHttpServletResponse();
        controller.login(request, start);
        String state = queryValue(start.getRedirectedUrl(), "state");
        when(client.userIdForCode("one-time-code")).thenReturn("staff_1");
        when(bindings.boundAdminUserId("staff_1")).thenReturn(null);
        when(client.member("staff_1")).thenReturn(
                new XingweiWeComDirectoryClient.Member("staff_1", "员工甲", "售前客服"));
        when(bindings.registerPending("staff_1", "员工甲")).thenReturn(2L);
        when(positionRoles.sync(2L, "售前客服")).thenReturn(true);
        when(bindings.loginUser("staff_1"))
                .thenReturn(User.withUsername("wecom_account").password("unused").roles("OPERATOR").build());

        var callback = new MockHttpServletResponse();
        controller.callback("one-time-code", state, request, callback);

        assertEquals("/admin/xingwei/work", callback.getRedirectedUrl());
        verify(positionRoles).sync(2L, "售前客服");
        assertEquals("wecom_account", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    private XingweiWeComSsoController controller(boolean enabled) {
        return new XingweiWeComSsoController(
                new XingweiWeComProperties("ww-test", "server-only-secret", "1000003", enabled, true),
                client, bindings, positionRoles, audits, "https://example.test");
    }

    private static String queryValue(String url, String key) {
        return java.util.Arrays.stream(URI.create(url).getRawQuery().split("&"))
                .filter(part -> part.startsWith(key + "="))
                .map(part -> part.substring(key.length() + 1))
                .findFirst().orElseThrow();
    }
}
