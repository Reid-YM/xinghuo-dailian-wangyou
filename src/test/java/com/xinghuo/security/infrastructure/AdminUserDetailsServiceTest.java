package com.xinghuo.security.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AdminUserDetailsServiceTest {

    @Test
    void mapsRepositoryUserIntoSpringSecurityDetailsWithoutChangingSemantics() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AdminUser user = AdminUser.builder()
                .username("operator")
                .passwordHash("$2a$12$hash")
                .role(AdminUser.Role.OPERATOR)
                .enabled(true)
                .build();
        when(repo.findByUsername("operator")).thenReturn(Optional.of(user));

        UserDetails details = new AdminUserDetailsService(repo).loadUserByUsername("operator");

        assertEquals("operator", details.getUsername());
        assertEquals("$2a$12$hash", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OPERATOR".equals(authority.getAuthority())));
        assertTrue(details.isEnabled());
    }

    @Test
    void disabledRepositoryUserStaysDisabled() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AdminUser user = AdminUser.builder()
                .username("admin")
                .passwordHash("hash")
                .role(AdminUser.Role.ADMIN)
                .enabled(false)
                .build();
        when(repo.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = new AdminUserDetailsService(repo).loadUserByUsername("admin");

        assertFalse(details.isEnabled());
    }

    @Test
    void missingUserKeepsUsernameNotFoundContract() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        when(repo.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> new AdminUserDetailsService(repo).loadUserByUsername("missing"));
    }
}