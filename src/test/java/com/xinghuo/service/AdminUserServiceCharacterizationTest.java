package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.model.AdminUser;
import com.xinghuo.repository.AdminUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class AdminUserServiceCharacterizationTest {

    private final AdminUserRepository repo = mock(AdminUserRepository.class);
    private final AdminUserService service = new AdminUserService(repo);

    @Test
    void mapsStoredAdminIntoSpringSecurityUserDetails() {
        AdminUser admin = AdminUser.builder()
                .username("root-admin")
                .passwordHash("{bcrypt}stored-hash")
                .role(AdminUser.Role.ADMIN)
                .enabled(true)
                .build();
        when(repo.findByUsername("root-admin")).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("root-admin");

        assertEquals("root-admin", details.getUsername());
        assertEquals("{bcrypt}stored-hash", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
        verify(repo).findByUsername("root-admin");
    }

    @Test
    void disabledOperatorKeepsRoleButIsDisabled() {
        AdminUser operator = AdminUser.builder()
                .username("operator-a")
                .passwordHash("hash-a")
                .role(AdminUser.Role.OPERATOR)
                .enabled(false)
                .build();
        when(repo.findByUsername("operator-a")).thenReturn(Optional.of(operator));

        UserDetails details = service.loadUserByUsername("operator-a");

        assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_OPERATOR".equals(authority.getAuthority())));
    }

    @Test
    void missingUserThrowsCurrentUsernameNotFoundException() {
        when(repo.findByUsername("missing-user")).thenReturn(Optional.empty());

        UsernameNotFoundException error = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing-user"));

        assertEquals("user not found", error.getMessage());
        verify(repo).findByUsername("missing-user");
    }
}
