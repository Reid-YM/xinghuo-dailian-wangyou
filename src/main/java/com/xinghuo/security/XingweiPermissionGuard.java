package com.xinghuo.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * 星维 V2C 混合权限与数据范围守卫。
 *
 * <p>安全策略：legacy ADMIN 始终全权；未配置星维角色的 legacy OPERATOR
 * 保留兼容只读能力与历史全量数据可见性；一旦明确分配星维角色，则同时按
 * xw_role_permissions 与角色 data_scope（ALL / DEPARTMENT / SELF）执行。
 * 该守卫只逐步接管新版星维接口，不改变旧 /admin 的 Spring Security 边界。
 */
@Component
public class XingweiPermissionGuard {
    private static final Set<String> LEGACY_OPERATOR_VIEW_PERMISSIONS = Set.of(
            "dashboard:view",
            "quote:view",
            "order:view",
            "service:view",
            "content:view",
            "org:view",
            "employee:view",
            "rbac:view",
            "channel:view",
            "wechat:view",
            "payment:view",
            "site:view",
            "seo:view",
            "audit:view",
            "deploy:view");

    private final JdbcTemplate jdbc;

    public XingweiPermissionGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean has(Authentication authentication, String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        AccessSnapshot snapshot = resolve(authentication);
        return snapshot.allowAll() || containsWithManageImplication(snapshot.permissionCodes(), permissionCode);
    }

    public boolean hasAny(Authentication authentication, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }
        for (String permissionCode : permissionCodes) {
            if (has(authentication, permissionCode)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAccessEmployee(Authentication authentication, long targetAdminUserId) {
        return canAccessEmployee(authentication, targetAdminUserId, employeeDepartmentCode(targetAdminUserId));
    }

    public boolean canAccessEmployee(
            Authentication authentication,
            long targetAdminUserId,
            String targetDepartmentCode) {
        AccessSnapshot snapshot = resolve(authentication);
        if ("DENIED".equals(snapshot.mode())) {
            return false;
        }
        if (snapshot.allowAll() || "ALL".equals(snapshot.dataScope())) {
            return true;
        }
        if (snapshot.adminUserId() == null) {
            return false;
        }
        if (snapshot.adminUserId() == targetAdminUserId) {
            return true;
        }
        if ("DEPARTMENT".equals(snapshot.dataScope())) {
            return snapshot.departmentCode() != null
                    && snapshot.departmentCode().equals(blankToNull(targetDepartmentCode));
        }
        return false;
    }

    public boolean canManageEmployee(Authentication authentication, long targetAdminUserId) {
        return has(authentication, "employee:manage")
                && canAccessEmployee(authentication, targetAdminUserId);
    }

    /**
     * 员工角色分配同时受三层约束：调用者必须有 rbac:manage、目标员工必须在数据范围内、
     * 目标当前角色范围与拟分配角色范围都不能宽于调用者。这样既防提权，也防较窄范围人员
     * 修改/移除更高范围员工的角色。
     */
    public boolean canAssignEmployeeRoles(
            Authentication authentication,
            long targetAdminUserId,
            List<String> requestedRoleCodes) {
        if (!has(authentication, "rbac:manage")
                || !canAccessEmployee(authentication, targetAdminUserId)) {
            return false;
        }
        AccessSnapshot snapshot = resolve(authentication);
        if (snapshot.allowAll() || "ALL".equals(snapshot.dataScope())) {
            return true;
        }

        String targetCurrentScope = employeeRoleDataScope(targetAdminUserId);
        if (!scopeCanGrant(snapshot.dataScope(), targetCurrentScope)) {
            return false;
        }
        if (requestedRoleCodes == null || requestedRoleCodes.isEmpty()) {
            return true;
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String requestedRoleCode : requestedRoleCodes) {
            String code = blankToNull(requestedRoleCode);
            if (code != null) {
                normalized.add(code.toUpperCase(Locale.ROOT));
            }
        }
        for (String roleCode : normalized) {
            String roleScope = roleDataScope(roleCode);
            if (roleScope == null || !scopeCanGrant(snapshot.dataScope(), roleScope)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 角色权限模板是全局配置。即使较窄范围角色未来获得 rbac:manage，也不能改写
     * 全局角色模板；只有 ALL 范围（或 legacy ADMIN 兜底）可以执行。
     */
    public boolean canManageGlobalRbac(Authentication authentication) {
        if (!has(authentication, "rbac:manage")) {
            return false;
        }
        AccessSnapshot snapshot = resolve(authentication);
        return snapshot.allowAll() || "ALL".equals(snapshot.dataScope());
    }

    /**
     * 员工档案写入除了校验目标员工，还校验变更后的中心不会越过当前 DEPARTMENT 范围。
     * ALL 可跨中心；SELF 只能修改本人；DEPARTMENT 只能把可见员工保留在自己的中心。
     */
    public boolean canManageEmployeeProfile(
            Authentication authentication,
            long targetAdminUserId,
            String requestedDepartmentCode,
            String requestedPositionCode) {
        if (!has(authentication, "employee:manage")
                || !canAccessEmployee(authentication, targetAdminUserId)) {
            return false;
        }
        AccessSnapshot snapshot = resolve(authentication);
        if (snapshot.allowAll() || "ALL".equals(snapshot.dataScope())) {
            return true;
        }
        if ("SELF".equals(snapshot.dataScope())) {
            return snapshot.adminUserId() != null && snapshot.adminUserId() == targetAdminUserId;
        }
        if (!"DEPARTMENT".equals(snapshot.dataScope()) || snapshot.departmentCode() == null) {
            return false;
        }

        String destinationDepartment = destinationDepartmentCode(requestedDepartmentCode, requestedPositionCode);
        return snapshot.departmentCode().equals(destinationDepartment);
    }

    public AccessSnapshot resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return denied();
        }

        Long adminUserId = adminUserId(authentication.getName());
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return new AccessSnapshot(
                    "LEGACY_ADMIN_OVERRIDE",
                    true,
                    allPermissionCodes(),
                    adminUserId == null ? List.of() : roleCodes(adminUserId),
                    "ALL",
                    adminUserId,
                    adminUserId == null ? null : employeeDepartmentCode(adminUserId));
        }
        if (!hasAuthority(authentication, "ROLE_OPERATOR") || adminUserId == null) {
            return denied();
        }

        List<RoleAssignment> assignments = roleAssignments(adminUserId);
        if (assignments.isEmpty()) {
            List<String> compatibility = new ArrayList<>(LEGACY_OPERATOR_VIEW_PERMISSIONS);
            compatibility.sort(String::compareTo);
            return new AccessSnapshot(
                    "LEGACY_OPERATOR_COMPAT",
                    false,
                    List.copyOf(compatibility),
                    List.of(),
                    "ALL",
                    adminUserId,
                    employeeDepartmentCode(adminUserId));
        }

        return new AccessSnapshot(
                "SHADOW_RBAC",
                false,
                permissionCodes(adminUserId),
                assignments.stream().map(RoleAssignment::code).toList(),
                effectiveDataScope(assignments),
                adminUserId,
                employeeDepartmentCode(adminUserId));
    }

    public Map<String, Object> asMap(Authentication authentication) {
        AccessSnapshot snapshot = resolve(authentication);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", snapshot.mode());
        result.put("allowAll", snapshot.allowAll());
        result.put("permissionCodes", snapshot.permissionCodes());
        result.put("roleCodes", snapshot.roleCodes());
        result.put("dataScope", snapshot.dataScope());
        result.put("adminUserId", snapshot.adminUserId());
        result.put("departmentCode", snapshot.departmentCode());
        result.put("legacyAdminOverride", "LEGACY_ADMIN_OVERRIDE".equals(snapshot.mode()));
        result.put("compatibilityMode", "LEGACY_OPERATOR_COMPAT".equals(snapshot.mode()));
        result.put("dataScopeEnforced", "SHADOW_RBAC".equals(snapshot.mode()));
        result.put(
                "departmentFallbackToSelf",
                "SHADOW_RBAC".equals(snapshot.mode())
                        && "DEPARTMENT".equals(snapshot.dataScope())
                        && snapshot.departmentCode() == null);
        return result;
    }

    private AccessSnapshot denied() {
        return new AccessSnapshot("DENIED", false, List.of(), List.of(), "NONE", null, null);
    }

    private Long adminUserId(String username) {
        try {
            return jdbc.queryForObject(
                    "SELECT id FROM admin_users WHERE username = ? AND enabled = TRUE",
                    Long.class,
                    username);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private List<String> roleCodes(long adminUserId) {
        return roleAssignments(adminUserId).stream().map(RoleAssignment::code).toList();
    }

    private List<RoleAssignment> roleAssignments(long adminUserId) {
        return jdbc.query(
                """
                SELECT r.code, r.data_scope
                FROM xw_admin_roles ar
                JOIN xw_rbac_roles r ON r.id = ar.role_id
                WHERE ar.admin_user_id = ? AND r.enabled = TRUE
                ORDER BY ar.is_primary DESC, r.sort_order, r.id
                """,
                (rs, rowNum) -> new RoleAssignment(
                        rs.getString("code"),
                        normalizeDataScope(rs.getString("data_scope"))),
                adminUserId);
    }

    private List<String> permissionCodes(long adminUserId) {
        List<String> rows = jdbc.query(
                """
                SELECT DISTINCT p.code
                FROM xw_admin_roles ar
                JOIN xw_rbac_roles r ON r.id = ar.role_id AND r.enabled = TRUE
                JOIN xw_role_permissions rp ON rp.role_id = r.id
                JOIN xw_permissions p ON p.id = rp.permission_id
                WHERE ar.admin_user_id = ?
                ORDER BY p.code
                """,
                (rs, rowNum) -> rs.getString("code"),
                adminUserId);
        return List.copyOf(new LinkedHashSet<>(rows));
    }

    private List<String> allPermissionCodes() {
        return jdbc.query(
                "SELECT code FROM xw_permissions ORDER BY code",
                (rs, rowNum) -> rs.getString("code"));
    }

    private String employeeDepartmentCode(long adminUserId) {
        try {
            return jdbc.queryForObject(
                    """
                    SELECT d.code
                    FROM admin_users u
                    LEFT JOIN xw_admin_profiles p ON p.admin_user_id = u.id
                    LEFT JOIN xw_departments d ON d.id = p.department_id AND d.enabled = TRUE
                    WHERE u.id = ? AND u.enabled = TRUE
                    """,
                    String.class,
                    adminUserId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String employeeRoleDataScope(long adminUserId) {
        List<RoleAssignment> assignments = roleAssignments(adminUserId);
        return assignments.isEmpty() ? "SELF" : effectiveDataScope(assignments);
    }

    private String destinationDepartmentCode(String requestedDepartmentCode, String requestedPositionCode) {
        String positionCode = blankToNull(requestedPositionCode);
        if (positionCode != null) {
            try {
                return jdbc.queryForObject(
                        """
                        SELECT d.code
                        FROM xw_positions p
                        JOIN xw_departments d ON d.id = p.department_id
                        WHERE p.code = ? AND p.enabled = TRUE AND d.enabled = TRUE
                        """,
                        String.class,
                        positionCode);
            } catch (EmptyResultDataAccessException ex) {
                return null;
            }
        }
        return blankToNull(requestedDepartmentCode);
    }

    private String roleDataScope(String roleCode) {
        try {
            return normalizeDataScope(jdbc.queryForObject(
                    "SELECT data_scope FROM xw_rbac_roles WHERE code = ? AND enabled = TRUE",
                    String.class,
                    roleCode));
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private static boolean scopeCanGrant(String callerScope, String requestedScope) {
        if ("ALL".equals(callerScope)) {
            return true;
        }
        if ("DEPARTMENT".equals(callerScope)) {
            return "DEPARTMENT".equals(requestedScope) || "SELF".equals(requestedScope);
        }
        return "SELF".equals(callerScope) && "SELF".equals(requestedScope);
    }

    private static String effectiveDataScope(List<RoleAssignment> assignments) {
        boolean department = false;
        for (RoleAssignment assignment : assignments) {
            if ("ALL".equals(assignment.dataScope())) {
                return "ALL";
            }
            if ("DEPARTMENT".equals(assignment.dataScope())) {
                department = true;
            }
        }
        return department ? "DEPARTMENT" : "SELF";
    }

    private static String normalizeDataScope(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return "SELF";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALL", "DEPARTMENT", "SELF" -> normalized;
            default -> "SELF";
        };
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static boolean containsWithManageImplication(List<String> permissions, String requested) {
        if (permissions.contains(requested)) {
            return true;
        }
        if (requested.endsWith(":view")) {
            String manage = requested.substring(0, requested.length() - ":view".length()) + ":manage";
            return permissions.contains(manage);
        }
        return false;
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private record RoleAssignment(String code, String dataScope) {}

    public record AccessSnapshot(
            String mode,
            boolean allowAll,
            List<String> permissionCodes,
            List<String> roleCodes,
            String dataScope,
            Long adminUserId,
            String departmentCode) {}
}
