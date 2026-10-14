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
                (6, 'selfscope', TRUE),
                (7, 'deptadmin', TRUE)
                """);
        jdbc.update("INSERT INTO xw_departments (id, code, enabled) VALUES (10, 'XW-03', TRUE), (20, 'XW-02', TRUE)");
        jdbc.update("INSERT INTO xw_positions (id, code, department_id, enabled) VALUES (100, 'POS-CS', 10, TRUE), (200, 'POS-OPS', 20, TRUE)");
        jdbc.update("INSERT INTO xw_admin_profiles (admin_user_id, department_id) VALUES (3, 10), (4, 10), (5, 20), (6, 20), (7, 10)");
        jdbc.update("""
                INSERT INTO xw_rbac_roles (id, code, data_scope, enabled, sort_order) VALUES
                (1, 'XW_MANAGEMENT', 'ALL', TRUE, 1),
                (2, 'XW_GENERAL_SUPPORT', 'DEPARTMENT', TRUE, 7),
                (3, 'XW_SELF_SERVICE', 'SELF', TRUE, 8),
                (4, 'XW_DEPT_ADMIN', 'DEPARTMENT', TRUE, 9)
                """);
        jdbc.update("INSERT INTO xw_permissions (id, code) VALUES (1, 'org:view'), (2, 'employee:manage'), (3, 'rbac:manage')");
        jdbc.update("INSERT INTO xw_admin_roles (admin_user_id, role_id, is_primary) VALUES (3, 2, TRUE), (4, 1, TRUE), (6, 3, TRUE), (7, 4, TRUE)");
        jdbc.update("INSERT INTO xw_role_permissions (role_id, permission_id) VALUES (2, 2), (3, 2), (3, 3), (4, 3)");
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
        assertTrue(guard.canManageGlobalRbac(admin));
        assertTrue(guard.canAssignEmployeeRoles(admin, 5L, List.of("XW_MANAGEMENT")));
    }

    @Test
    void roleAdminOverrideStaysAllEvenWhenPrincipalIsNotInLegacyUserTable() {
        Authentication root = authentication("root-not-in-db", "ROLE_ADMIN");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(root);

        assertTrue(snapshot.allowAll());
        assertEquals("ALL", snapshot.dataScope());
        assertTrue(guard.canAccessEmployee(root, 5L));
        assertTrue(guard.canManageGlobalRbac(root));
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
        assertTrue(guard.has(operator, "payment:view"));
        assertFalse(guard.has(operator, "payment:manage"));
        assertTrue(guard.canAccessEmployee(operator, 5L));
        assertFalse(guard.has(operator, "employee:manage"));
        assertFalse(guard.has(operator, "rbac:manage"));
        assertFalse(guard.canManageGlobalRbac(operator));
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
    void paymentWriteRequiresExplicitAssignedPermissionOrAdminOverride() {
        jdbc.update("INSERT INTO xw_permissions (id, code) VALUES (20, 'payment:view'), (21, 'payment:manage')");
        jdbc.update("INSERT INTO xw_role_permissions (role_id, permission_id) VALUES (1, 21), (2, 20)");

        assertTrue(guard.has(authentication("admin", "ROLE_ADMIN"), "payment:manage"));
        assertTrue(guard.has(authentication("operator", "ROLE_OPERATOR"), "payment:view"));
        assertFalse(guard.has(authentication("operator", "ROLE_OPERATOR"), "payment:manage"));
        assertTrue(guard.has(authentication("scoped", "ROLE_OPERATOR"), "payment:view"));
        assertFalse(guard.has(authentication("scoped", "ROLE_OPERATOR"), "payment:manage"));
        assertTrue(guard.has(authentication("peer", "ROLE_OPERATOR"), "payment:manage"));
        assertTrue(guard.has(authentication("peer", "ROLE_OPERATOR"), "payment:view"));
    }

    @Test
    void departmentRbacManagerCannotEscalateOrModifyBroaderTargetRole() {
        Authentication manager = authentication("deptadmin", "ROLE_OPERATOR");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(manager);

        assertEquals("DEPARTMENT", snapshot.dataScope());
        assertTrue(guard.has(manager, "rbac:manage"));
        assertTrue(guard.canAssignEmployeeRoles(manager, 3L, List.of("XW_GENERAL_SUPPORT")));
        assertTrue(guard.canAssignEmployeeRoles(manager, 3L, List.of("XW_SELF_SERVICE")));
        assertTrue(guard.canAssignEmployeeRoles(manager, 3L, List.of()));
        assertFalse(guard.canAssignEmployeeRoles(manager, 3L, List.of("XW_MANAGEMENT")));
        assertTrue(guard.canAccessEmployee(manager, 4L));
        assertFalse(guard.canAssignEmployeeRoles(manager, 4L, List.of("XW_GENERAL_SUPPORT")),
                "same-department employee with an existing ALL role must remain protected");
        assertFalse(guard.canAssignEmployeeRoles(manager, 5L, List.of("XW_GENERAL_SUPPORT")));
        assertFalse(guard.canAssignEmployeeRoles(manager, 3L, List.of("MISSING_ROLE")));
        assertFalse(guard.canManageGlobalRbac(manager));
    }

    @Test
    void selfScopeCanOnlyAssignSelfScopeRoleToSelf() {
        Authentication self = authentication("selfscope", "ROLE_OPERATOR");
        XingweiPermissionGuard.AccessSnapshot snapshot = guard.resolve(self);

        assertEquals("SELF", snapshot.dataScope());
        assertEquals("XW-02", snapshot.departmentCode());
        assertTrue(guard.has(self, "rbac:manage"));
        assertTrue(guard.canAccessEmployee(self, 6L));
        assertFalse(guard.canAccessEmployee(self, 5L));
        assertTrue(guard.canManageEmployee(self, 6L));
        assertFalse(guard.canManageEmployee(self, 5L));
        assertTrue(guard.canManageEmployeeProfile(self, 6L, "XW-03", null),
                "SELF scope stays limited to the same account even if profile metadata changes");
        assertTrue(guard.canAssignEmployeeRoles(self, 6L, List.of("XW_SELF_SERVICE")));
        assertFalse(guard.canAssignEmployeeRoles(self, 6L, List.of("XW_GENERAL_SUPPORT")));
        assertFalse(guard.canAssignEmployeeRoles(self, 6L, List.of("XW_MANAGEMENT")));
        assertFalse(guard.canAssignEmployeeRoles(self, 5L, List.of("XW_SELF_SERVICE")));
        assertFalse(guard.canManageGlobalRbac(self));
    }

    @Test
    void unknownOrAnonymousPrincipalIsDenied() {
        Authentication unknown = authentication("missing", "ROLE_OPERATOR");
        assertEquals("DENIED", guard.resolve(unknown).mode());
        assertEquals("NONE", guard.resolve(unknown).dataScope());
        assertFalse(guard.has(unknown, "org:view"));
        assertFalse(guard.canAccessEmployee(unknown, 3L));
        assertFalse(guard.canAssignEmployeeRoles(unknown, 3L, List.of("XW_SELF_SERVICE")));
        assertFalse(guard.canManageGlobalRbac(unknown));
        assertFalse(guard.has(null, "org:view"));
    }

    private static Authentication authentication(String username, String authority) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
