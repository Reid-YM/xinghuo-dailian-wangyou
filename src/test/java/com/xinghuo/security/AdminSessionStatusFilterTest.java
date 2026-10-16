package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.xinghuo.model.AdminUser;
import com.xinghuo.repository.AdminUserRepository;
import com.xinghuo.service.AdminUserService;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class AdminSessionStatusFilterTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void revisionSurvivesCredentialErasureAndPasswordResetRevokesOldPrincipal() throws Exception {
        var repo=mock(AdminUserRepository.class);var stored=AdminUser.builder().username("employee").passwordHash("STORED-HASH").role(AdminUser.Role.OPERATOR).enabled(true).build();
        when(repo.findByUsername("employee")).thenReturn(Optional.of(stored));var users=new AdminUserService(repo);
        var principal=users.loadUserByUsername("employee");var auth=UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());auth.eraseCredentials();
        assertNull(principal.getPassword());assertTrue(users.sessionCurrent(auth));
        stored.setAuthVersion(1);assertFalse(users.sessionCurrent(auth));
        SecurityContextHolder.getContext().setAuthentication(auth);var request=new MockHttpServletRequest("GET","/api/admin/xingwei/work");var session=request.getSession();var response=new MockHttpServletResponse();var reached=new AtomicBoolean();
        new AdminSessionStatusFilter(users).doFilter(request,response,(req,res)->reached.set(true));
        assertFalse(reached.get());assertEquals(401,response.getStatus());assertTrue(((MockHttpSession)session).isInvalid());assertNull(SecurityContextHolder.getContext().getAuthentication());
        var fresh=users.loadUserByUsername("employee");assertEquals(1,((AdminLoginPrincipal)fresh).authVersion());assertTrue(users.sessionCurrent(UsernamePasswordAuthenticationToken.authenticated(fresh,null,fresh.getAuthorities())));
    }
    @Test void disabledOrChangedRoleIsRejectedAndLegacyPrincipalWorksOnlyAtRevisionZero() throws Exception {
        var repo=mock(AdminUserRepository.class);var stored=AdminUser.builder().username("admin").passwordHash("HASH").role(AdminUser.Role.ADMIN).enabled(true).build();when(repo.findByUsername("admin")).thenReturn(Optional.of(stored));var users=new AdminUserService(repo);
        var legacy=User.withUsername("admin").password("erased").roles("ADMIN").build();var auth=UsernamePasswordAuthenticationToken.authenticated(legacy,null,legacy.getAuthorities());assertTrue(users.sessionCurrent(auth));
        stored.setAuthVersion(1);assertFalse(users.sessionCurrent(auth));stored.setAuthVersion(0);stored.setRole(AdminUser.Role.OPERATOR);assertFalse(users.sessionCurrent(auth));stored.setRole(AdminUser.Role.ADMIN);stored.setEnabled(false);assertFalse(users.sessionCurrent(auth));
        SecurityContextHolder.getContext().setAuthentication(auth);var response=new MockHttpServletResponse();new AdminSessionStatusFilter(users).doFilter(new MockHttpServletRequest("GET","/admin/xingwei"),response,(req,res)->fail("revoked administrator reached controller"));assertEquals("/admin/login?expired",response.getRedirectedUrl());
    }
    @Test void anonymousAndPublicWebsiteRequestsRemainOnTheirExistingAuthenticationPath() throws Exception {
        var users=mock(AdminUserService.class);var filter=new AdminSessionStatusFilter(users);var reached=new AtomicBoolean();filter.doFilter(new MockHttpServletRequest("GET","/admin/login"),new MockHttpServletResponse(),(req,res)->reached.set(true));assertTrue(reached.get());
        var principal=User.withUsername("staff").password("unused").roles("OPERATOR").build();SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));filter.doFilter(new MockHttpServletRequest("GET","/api/account/me"),new MockHttpServletResponse(),(req,res)->{});verifyNoInteractions(users);
    }
}
