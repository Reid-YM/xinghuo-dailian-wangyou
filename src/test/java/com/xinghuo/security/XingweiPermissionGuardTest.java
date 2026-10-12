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
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(32), enabled BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_positions (id BIGINT PRIMARY KEY, code VARCHAR(48), department_id BIGINT, enabled BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_admin_profiles (admin_user_id BIGINT PRIMARY KEY, department_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_rbac_roles (id BIGINT PRIMARY KEY, code VARCHAR(48), data_scope VARCHAR(20), enabled BOOLEAN NOT NULL, sort_order INT NOT NULL)");
        jdbc.execute("CREATE TABLE xw_permissions (id BIGINT PRIMARY KEY, code VARCHAR(64))");
        jdbc.execute("CREATE TABLE xw_admin_roles (admin_user_id BIGINT, role_id BIGINT, is_primary BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_role_permissions (role_id BIGINT, permission_id BIGINT)");

        jdbc.update("""
                INSERT INTO admin_users (id, username, enabled) VALUES
                (1, 'admin', TRUE),
                (2, 'operator', TRUE),
                (3, 'scoped', TRUE),
                (4, 'peer', TRUE),
                (5, 'outsider', TRUE),
                (6, 'selfscope', TRUE)
                """);
        jdbc.update("INSERT INTO xw_departments (id, code, enabled) VALUES (10, 'XW-03', TRUE), (20, 'XW-02', TRUE)");
        jdbc.update("INSERT INTO xw_positions (id, code, department_id, enabled) VALUES (100, 'POS-CS', 10, TRUE), (200, 'POS-OPS', 20, TRUE)");
        jdbc.update("INSERT INTO xw_admin_profiles (admin_user_id, department_id) VALUES (3, 10), (4, 10), (5, 20), (6, 20)");
        jdbc.update("""
                INSERT INTO xw_rbac_roles (id, code, data_scope, enabled, sort_order) VALUES
                (1, 'XW_MANAGEMENT', 'ALL', TRUE, 1),
                (2, 'XW_GENERAL_SUPPORT', 'DEPARTMENT', TRUE, 7),
                (3, 'XW_SELF_SERVICE', 'SELF', TRUE, 8)
                """);
        jdbc.update("INSERT INTO xw_permissions (id, code) VALUES (1, 'org:view'), (2, 'employee:manage'), (3, 'rbac:manage')");
        jdbc.update("INSERT INTO xw_admin_roles (admin_user_id, role_id, is_primary) VALUES (3, 2, TRUE), (6, 3, TRUE)");
        jdbc.update("INSERT INTO xw_role_permissions (role_id, permission_id) VALUES (2, 2), (3, 2)");
        guard = new XingweiPermissionGuard(jdbc);
    }

    @Test
    void legacyAdminAlwaysHasSafetyOverrideAndAllDataScope() {
        Authentication admin = authentication("admin", "ROLE_ADMIN");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(admin);

        assertEquals("LEGACY_ADMIN_OVERRIDE", snapshot.mode());
        assertEquals("ALL", snapshot.dataScope());
        assertEquals(1L, snapshot.adminUserId());
        assertTrue(snapshot.allowAll());
        assertTrue(guard.has(admin, "rbac:manage"));
        assertTrue(guard.has(admin, "future:permission"));
        assertTrue(guard.canAccessEmployee(admin, 5L));
    }

    @Test
    void unassignedOperatorKeepsReadOnlyCompatibilityAndHistoricalAllScope() {
        Authentication operator = authentication("operator", "ROLE_OPERATOR");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(operator);

        assertEquals("LEGACY_OPERATOR_COMPAT", snapshot.mode());
        assertEquals("ALL", snapshot.dataScope());
        assertFalse(snapshot.allowAll());
        assertTrue(guard.has(operator, "org:view"));
        assertTrue(guard.has(operator, "order:view"));
        assertTrue(guard.canAccessEmployee(operator, 5L));
        assertFalse(guard.has(operator, "employee:manage"));
        assertFalse(guard.has(operator, "rbac:manage"));
    }

    @Test
    void assignedOperatorUsesRolePermissionsAndDepartmentScope() {
        Authentication operator = authentication("scoped", "ROLE_OPERATOR");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(operator);

        assertEquals("SHADOW_RBAC", snapshot.mode());
        assertEquals(List.of("XW_GENERAL_SUPPORT"), snapshot.roleCodes());
        assertEquals("DEPARTMENT", snapshot.dataScope());
        assertEquals("XW-03", snapshot.departmentCode());
        assertTrue(guard.has(operator, "employee:manage"));
        assertTrue(guard.has(operator, "employee:view"));
        assertTrue(guard.hasAny(operator, "org:view", "employee:view"));
        assertFalse(guard.has(operator, "org:view"));
        assertFalse(guard.has(operator, "rbac:manage"));

        assertTrue(guard.canAccessEmployee(operator, 3L));
        assertTrue(guard.canAccessEmployee(operator, 4L));
        assertFalse(guard.canAccessEmployee(operator, 5L));
        assertTrue(guard.canManageEmployee(operator, 4L));
        assertFalse(guard.canManageEmployee(operator, 5L));
        assertTrue(guard.canManageEmployeeProfile(operator, 4L, "XW-03", null));
        assertTrue(guard.canManageEmployeeProfile(operator, 4L, null, "POS-CS"));
        assertFalse(guard.canManageEmployeeProfile(operator, 4L, "XW-02", null));
        assertFalse(guard.canManageEmployeeProfile(operator, 4L, null, "POS-OPS"));
        assertFalse(guard.canManageEmployeeProfile(operator, 4L, null, null));
    }

    @Test
    void selfScopeOnlySeesAndMutatesOwnEmployeeRecord() {
        Authentication self = authentication("selfscope", "ROLE_OPERATOR");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(self);

        assertEquals("SELF", snapshot.dataScope());
        assertEquals("XW-02", snapshot.departmentCode());
        assertTrue(guard.canAccessEmployee(self, 6L));
        assertFalse(guard.canAccessEmployee(self, 5L));
        assertTrue(guard.canManageEmployee(self, 6L));
        assertFalse(guard.canManageEmployee(self, 5L));
        assertTrue(guard.canManageEmployeeProfile(self, 6L, "XW-03", null),
                "SELF scope stays limited to the same account even if profile metadata changes");
    }

    @Test
    void unknownOrAnonymousPrincipalIsDenied() {
        Authentication unknown = authentication("missing", "ROLE_OPERATOR");
        assertEquals("DENIED", guard.resolve(unknown).mode());
        assertEquals("NONE", guard.resolve(unknown).dataScope());
        assertFalse(guard.has(unknown, "org:view"));
        assertFalse(guard.canAccessEmployee(unknown, 3L));
        assertFalse(guard.has(null, "org:view"));
    }

    private static Authentication authentication(String username, String authority) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
