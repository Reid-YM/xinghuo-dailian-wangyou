package com.xinghuo.security;

import com.xinghuo.service.AdminUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Applies to password and WeCom principals before any privileged request is authorized. */
public final class AdminSessionStatusFilter extends OncePerRequestFilter {
    private final AdminUserService users;
    public AdminSessionStatusFilter(AdminUserService users){this.users=users;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if((path.equals("/admin")||path.startsWith("/admin/")||path.equals("/api/admin")||path.startsWith("/api/admin/"))
            && auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken)&&!users.sessionCurrent(auth)){
            SecurityContextHolder.clearContext();var session=request.getSession(false);if(session!=null)session.invalidate();
            response.setHeader("Cache-Control","no-store");
            if(path.startsWith("/api/"))response.sendError(401);else response.sendRedirect(request.getContextPath()+"/admin/login?expired");
            return;
        }
        chain.doFilter(request,response);
    }
}
