package com.xinghuo.wecom;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revokes sessions created by WeCom SSO when a binding, account, or employee status changes. */
public class XingweiWeComSessionFilter extends OncePerRequestFilter {
    private final XingweiWeComBindingService bindings;
    private final XingweiWeComProperties properties;

    public XingweiWeComSessionFilter(XingweiWeComBindingService bindings, XingweiWeComProperties properties) {
        this.bindings = bindings;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Object ssoUser = session == null ? null : session.getAttribute(XingweiWeComSsoController.SSO_SESSION_KEY);
        if (ssoUser instanceof XingweiWeComSsoController.SsoIdentity identity) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!properties.ssoReady() || authentication == null
                    || !identity.username().equals(authentication.getName())
                    || authentication.getAuthorities().stream().noneMatch(
                            authority -> ("ROLE_" + identity.role()).equals(authority.getAuthority()))
                    || !bindings.activeSsoUser(
                            identity.username(), identity.wecomUserId(), identity.role())) {
                SecurityContextHolder.clearContext();
                session.invalidate();
                if (path.equals("/admin/xingwei/wecom/login")) {
                    chain.doFilter(request, response);
                    return;
                }
                if (request.getRequestURI().startsWith("/api/")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                } else {
                    response.sendRedirect("/admin/login?wecom=expired");
                }
                return;
            }
            // Only the native workspace and redirects into it are available to SSO sessions.
            boolean legacyAdminPage = (path.equals("/admin") || path.startsWith("/admin/"))
                    && !(path.equals("/admin/xingwei") || path.startsWith("/admin/xingwei/"))
                    && !(path.equals("/admin") || path.equals("/admin/guides")
                            || path.equals("/admin/frontend-controls") || path.equals("/admin/login"));
            boolean legacyAdminApi = (path.equals("/api/admin") || path.startsWith("/api/admin/"))
                    && !(path.equals("/api/admin/xingwei") || path.startsWith("/api/admin/xingwei/"));
            if (legacyAdminPage || legacyAdminApi) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
