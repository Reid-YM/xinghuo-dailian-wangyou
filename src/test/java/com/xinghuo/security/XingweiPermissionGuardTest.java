package com.xinghuo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class XingweiPermissionGuardTest {
    private JdbcTemplate jdbc;
    private XingweiPermissionGuard guard;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:xw_permission_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE admin_users (id BIGINT PRIMARY KEY, username VARCHAR(64) UNIQUE, enabled BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_rbac_roles (id BIGINT PRIMARY KEY, code VARCHAR(48), enabled BOOLEAN NOT NULL, sort_order INT NOT NULL)");
        jdbc.execute("CREATE TABLE xw_permissions (id BIGINT PRIMARY KEY, code VARCHAR(64))");
        jdbc.execute("CREATE TABLE xw_admin_roles (admin_user_id BIGINT, role_id BIGINT, is_primary BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_role_permissions (role_id BIGINT, permission_id BIGINT)");

        jdbc.update("INSERT INTO admin_users (id, username, enabled) VALUES (1, 'admin', TRUE), (2, 'operator', TRUE), (3, 'scoped', TRUE)");
        jdbc.update("INSERT INTO xw_rbac_roles (id, code, enabled, sort_order) VALUES (1, 'XW_MANAGEMENT', TRUE, 1), (2, 'XW_GENERAL_SUPPORT', TRUE, 7)");
        jdbc.update("INSERT INTO xw_permissions (id, code) VALUES (1, 'org:view'), (2, 'employee:manage'), (3, 'rbac:manage')");
        jdbc.update("INSERT INTO xw_admin_roles (admin_user_id, role_id, is_primary) VALUES (3, 2, TRUE)");
        jdbc.update("INSERT INTO xw_role_permissions (role_id, permission_id) VALUES (2, 2)");
        guard = new XingweiPermissionGuard(jdbc);
    }

    @Test
    void legacyAdminAlwaysHasSafetyOverride() {
        Authentication admin = authentication("admin", "ROLE_ADMIN");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(admin);

        assertEquals("LEGACY_ADMIN_OVERRIDE", snapshot.mode());
        assertTrue(snapshot.allowAll());
        assertTrue(guard.has(admin, "rbac:manage"));
        assertTrue(guard.has(admin, "future:permission"));
    }

    @Test
    void unassignedOperatorKeepsReadOnlyCompatibilityButCannotMutateV2b() {
        Authentication operator = authentication("operator", "ROLE_OPERATOR");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(operator);

        assertEquals("LEGACY_OPERATOR_COMPAT", snapshot.mode());
        assertFalse(snapshot.allowAll());
        assertTrue(guard.has(operator, "org:view"));
        assertTrue(guard.has(operator, "order:view"));
        assertFalse(guard.has(operator, "employee:manage"));
        assertFalse(guard.has(operator, "rbac:manage"));
    }

    @Test
    void assignedOperatorUsesOnlyRolePermissionsAndManageImpliesView() {
        Authentication operator = authentication("scoped", "ROLE_OPERATOR");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(operator);

        assertEquals("SHADOW_RBAC", snapshot.mode());
        assertEquals(List.of("XW_GENERAL_SUPPORT"), snapshot.roleCodes());
        assertTrue(guard.has(operator, "employee:manage"));
        assertTrue(guard.has(operator, "employee:view"));
        assertTrue(guard.hasAny(operator, "org:view", "employee:view"));
        assertFalse(guard.has(operator, "org:view"));
        assertFalse(guard.has(operator, "rbac:manage"));
    }

    @Test
    void unknownOrAnonymousPrincipalIsDenied() {
        Authentication unknown = authentication("missing", "ROLE_OPERATOR");
        assertEquals("DENIED", guard.resolve(unknown).mode());
        assertFalse(guard.has(unknown, "org:view"));
        assertFalse(guard.has(null, "org:view"));
    }

    private static Authentication authentication(String username, String authority) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
