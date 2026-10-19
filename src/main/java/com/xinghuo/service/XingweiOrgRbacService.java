package com.xinghuo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XingweiOrgRbacService {
    private static final Set<String> PROFILE_STATUS = Set.of("ACTIVE", "ON_LEAVE", "INACTIVE");
    private final JdbcTemplate jdbc;

    public XingweiOrgRbacService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> snapshot() {
        List<Map<String, Object>> departments = departments();
        List<Map<String, Object>> positions = positions();
        List<Map<String, Object>> employees = employees();
        List<Map<String, Object>> roles = roles();
        List<Map<String, Object>> permissions = permissions();

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("departments", departments.size());
        counts.put("positions", positions.size());
        counts.put("employees", employees.size());
        counts.put("assignedEmployees", employees.stream()
                .filter(row -> row.get("departmentCode") != null || row.get("positionCode") != null)
                .count());
        counts.put("roles", roles.size());
        counts.put("permissions", permissions.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shadowMode", true);
        result.put("enforcement", "LEGACY_ADMIN_ROLE");
        result.put("counts", counts);
        result.put("departments", departments);
        result.put("positions", positions);
        result.put("employees", employees);
        result.put("roles", roles);
        result.put("permissions", permissions);
        return result;
    }

    public List<Map<String, Object>> departments() {
        return jdbc.query(
                """
                SELECT d.id, d.code, d.name, d.sort_order, d.built_in, d.enabled, COUNT(p.id) AS position_count
                FROM xw_departments d
                LEFT JOIN xw_positions p ON p.department_id = d.id AND p.enabled = TRUE
                GROUP BY d.id, d.code, d.name, d.sort_order, d.built_in, d.enabled
                ORDER BY d.sort_order, d.id
                """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("code", rs.getString("code"));
                    row.put("name", rs.getString("name"));
                    row.put("sortOrder", rs.getInt("sort_order"));
                    row.put("builtIn", rs.getBoolean("built_in"));
                    row.put("enabled", rs.getBoolean("enabled"));
                    row.put("positionCount", rs.getLong("position_count"));
                    return row;
                });
    }

    public List<Map<String, Object>> positions() {
        return jdbc.query(
                """
                SELECT p.id, p.code, p.name, p.category, p.sort_order, p.built_in, p.enabled,
                       d.id AS department_id, d.code AS department_code, d.name AS department_name
                FROM xw_positions p
                JOIN xw_departments d ON d.id = p.department_id
                ORDER BY d.sort_order, p.sort_order, p.id
                """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("code", rs.getString("code"));
                    row.put("name", rs.getString("name"));
                    row.put("category", rs.getString("category"));
                    row.put("sortOrder", rs.getInt("sort_order"));
                    row.put("builtIn", rs.getBoolean("built_in"));
                    row.put("enabled", rs.getBoolean("enabled"));
                    row.put("departmentId", rs.getLong("department_id"));
                    row.put("departmentCode", rs.getString("department_code"));
                    row.put("departmentName", rs.getString("department_name"));
                    return row;
                });
    }

    public List<Map<String, Object>> employees() {
        List<Map<String, Object>> rows = jdbc.query(
                """
                SELECT u.id, u.username, u.role AS legacy_role, u.enabled AS login_enabled,
                       p.employee_no, p.display_name, p.status, p.notes, p.wecom_user_id,
                       d.code AS department_code, d.name AS department_name,
                       pos.code AS position_code, pos.name AS position_name
                FROM admin_users u
                LEFT JOIN xw_admin_profiles p ON p.admin_user_id = u.id
                LEFT JOIN xw_departments d ON d.id = p.department_id
                LEFT JOIN xw_positions pos ON pos.id = p.position_id
                ORDER BY u.id
                """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("username", rs.getString("username"));
                    row.put("legacyRole", rs.getString("legacy_role"));
                    row.put("loginEnabled", rs.getBoolean("login_enabled"));
                    row.put("employeeNo", rs.getString("employee_no"));
                    row.put("displayName", rs.getString("display_name"));
                    row.put("status", rs.getString("status"));
                    row.put("wecomUserId", rs.getString("wecom_user_id"));
                    row.put("departmentCode", rs.getString("department_code"));
                    row.put("departmentName", rs.getString("department_name"));
                    row.put("positionCode", rs.getString("position_code"));
                    row.put("positionName", rs.getString("position_name"));
                    row.put("roleCodes", new ArrayList<String>());
                    return row;
                });

        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byId.put(((Number) row.get("id")).longValue(), row);
        }
        jdbc.query(
                """
                SELECT ar.admin_user_id, r.code
                FROM xw_admin_roles ar
                JOIN xw_rbac_roles r ON r.id = ar.role_id
                ORDER BY ar.admin_user_id, ar.is_primary DESC, r.sort_order, r.id
                """,
                rs -> {
                    Map<String, Object> employee = byId.get(rs.getLong("admin_user_id"));
                    if (employee != null) {
                        @SuppressWarnings("unchecked")
                        List<String> roleCodes = (List<String>) employee.get("roleCodes");
                        roleCodes.add(rs.getString("code"));
                    }
                });
        return rows;
    }

    public List<Map<String, Object>> roles() {
        List<Map<String, Object>> rows = jdbc.query(
                """
                SELECT id, code, name, data_scope, sort_order, built_in, enabled
                FROM xw_rbac_roles
                ORDER BY sort_order, id
                """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("code", rs.getString("code"));
                    row.put("name", rs.getString("name"));
                    row.put("dataScope", rs.getString("data_scope"));
                    row.put("sortOrder", rs.getInt("sort_order"));
                    row.put("builtIn", rs.getBoolean("built_in"));
                    row.put("enabled", rs.getBoolean("enabled"));
                    row.put("permissionCodes", new ArrayList<String>());
                    return row;
                });

        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            byId.put(((Number) row.get("id")).longValue(), row);
        }
        jdbc.query(
                """
                SELECT rp.role_id, p.code
                FROM xw_role_permissions rp
                JOIN xw_permissions p ON p.id = rp.permission_id
                ORDER BY rp.role_id, p.id
                """,
                rs -> {
                    Map<String, Object> role = byId.get(rs.getLong("role_id"));
                    if (role != null) {
                        @SuppressWarnings("unchecked")
                        List<String> permissionCodes = (List<String>) role.get("permissionCodes");
                        permissionCodes.add(rs.getString("code"));
                    }
                });
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            List<String> codes = (List<String>) row.get("permissionCodes");
            row.put("permissionCount", codes.size());
        }
        return rows;
    }

    public List<Map<String, Object>> permissions() {
        return jdbc.query(
                """
                SELECT id, code, name, module_code, built_in
                FROM xw_permissions
                ORDER BY module_code, id
                """,
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("code", rs.getString("code"));
                    row.put("name", rs.getString("name"));
                    row.put("module", rs.getString("module_code"));
                    row.put("builtIn", rs.getBoolean("built_in"));
                    return row;
                });
    }

    public Map<String, Object> employee(long adminUserId) {
        return employees().stream()
                .filter(row -> ((Number) row.get("id")).longValue() == adminUserId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("管理员账号不存在"));
    }

    public Map<String, Object> role(String roleCode) {
        String normalized = normalizeCode(roleCode, 48, "角色编码");
        return roles().stream()
                .filter(row -> normalized.equals(row.get("code")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
    }

    @Transactional
    public Map<String, Object> updateEmployeeProfile(long adminUserId, ProfileInput input) {
        requireAdminUser(adminUserId);
        String displayName = clean(input.displayName(), 80, "姓名");
        String employeeNo = clean(input.employeeNo(), 32, "员工编号");
        String notes = clean(input.notes(), 500, "备注");
        String status = blankToNull(input.status());
        if (status == null) {
            status = "ACTIVE";
        }
        status = status.toUpperCase(Locale.ROOT);
        if (!PROFILE_STATUS.contains(status)) {
            throw new IllegalArgumentException("员工状态仅支持 ACTIVE / ON_LEAVE / INACTIVE");
        }

        String departmentCode = blankToNull(input.departmentCode());
        String positionCode = blankToNull(input.positionCode());
        Long departmentId = null;
        Long positionId = null;

        if (positionCode != null) {
            Map<String, Object> position = jdbc.queryForMap(
                    """
                    SELECT p.id AS position_id, d.id AS department_id, d.code AS department_code
                    FROM xw_positions p
                    JOIN xw_departments d ON d.id = p.department_id
                    WHERE p.code = ? AND p.enabled = TRUE AND d.enabled = TRUE
                    """,
                    positionCode);
            positionId = ((Number) position.get("position_id")).longValue();
            departmentId = ((Number) position.get("department_id")).longValue();
            String positionDepartmentCode = String.valueOf(position.get("department_code"));
            if (departmentCode != null && !departmentCode.equals(positionDepartmentCode)) {
                throw new IllegalArgumentException("岗位与所属中心不匹配");
            }
            departmentCode = positionDepartmentCode;
        } else if (departmentCode != null) {
            departmentId = jdbc.queryForObject(
                    "SELECT id FROM xw_departments WHERE code = ? AND enabled = TRUE",
                    Long.class,
                    departmentCode);
        }

        List<long[]> autoRoles = jdbc.query(
                "SELECT position_id, role_id FROM xw_wecom_auto_roles WHERE admin_user_id = ?",
                (rs, n) -> new long[] {rs.getLong("position_id"), rs.getLong("role_id")}, adminUserId);
        if (!autoRoles.isEmpty() && !java.util.Objects.equals(positionId, autoRoles.getFirst()[0])) {
            jdbc.update("DELETE FROM xw_admin_roles WHERE admin_user_id = ? AND role_id = ?",
                    adminUserId, autoRoles.getFirst()[1]);
            jdbc.update("DELETE FROM xw_wecom_auto_roles WHERE admin_user_id = ?", adminUserId);
        }

        try {
            int updated = jdbc.update(
                    """
                    UPDATE xw_admin_profiles
                    SET employee_no = ?, display_name = ?, department_id = ?, position_id = ?,
                        status = ?, notes = CASE WHEN ? IS NULL THEN notes ELSE ? END, updated_at = NOW(6)
                    WHERE admin_user_id = ?
                    """,
                    employeeNo, displayName, departmentId, positionId, status, input.notes(), notes, adminUserId);
            if (updated == 0) {
                jdbc.update(
                        """
                        INSERT INTO xw_admin_profiles
                            (admin_user_id, employee_no, display_name, department_id, position_id,
                             status, notes, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, NOW(6), NOW(6))
                        """,
                        adminUserId, employeeNo, displayName, departmentId, positionId, status, notes);
            }
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("员工编号已被其他账号使用");
        }
        return employee(adminUserId);
    }

    @Transactional
    public Map<String, Object> replaceEmployeeRoles(long adminUserId, List<String> roleCodes) {
        requireAdminUser(adminUserId);
        List<String> normalized = distinctCodes(roleCodes, 48, "角色编码");
        validateCodes("xw_rbac_roles", normalized, "角色");
        jdbc.update("DELETE FROM xw_wecom_auto_roles WHERE admin_user_id = ?", adminUserId);
        jdbc.update("DELETE FROM xw_admin_roles WHERE admin_user_id = ?", adminUserId);
        for (int i = 0; i < normalized.size(); i++) {
            jdbc.update(
                    """
                    INSERT INTO xw_admin_roles (admin_user_id, role_id, is_primary, created_at)
                    SELECT ?, id, ?, NOW(6) FROM xw_rbac_roles WHERE code = ? AND enabled = TRUE
                    """,
                    adminUserId, i == 0, normalized.get(i));
        }
        return employee(adminUserId);
    }

    @Transactional
    public Map<String, Object> replaceRolePermissions(String roleCode, List<String> permissionCodes) {
        String normalizedRole = normalizeCode(roleCode, 48, "角色编码");
        if ("XW_MANAGEMENT".equals(normalizedRole)) {
            throw new IllegalArgumentException("管理层角色是安全兜底角色，不允许修改权限");
        }
        Long roleId = jdbc.queryForObject(
                "SELECT id FROM xw_rbac_roles WHERE code = ? AND enabled = TRUE",
                Long.class,
                normalizedRole);
        if (roleId == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        List<String> normalized = distinctCodes(permissionCodes, 64, "权限编码");
        validateCodes("xw_permissions", normalized, "权限");
        jdbc.update("DELETE FROM xw_role_permissions WHERE role_id = ?", roleId);
        for (String permissionCode : normalized) {
            jdbc.update(
                    """
                    INSERT INTO xw_role_permissions (role_id, permission_id, created_at)
                    SELECT ?, id, NOW(6) FROM xw_permissions WHERE code = ?
                    """,
                    roleId, permissionCode);
        }
        return role(normalizedRole);
    }

    private void requireAdminUser(long adminUserId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM admin_users WHERE id = ?", Long.class, adminUserId);
        if (count == null || count == 0L) {
            throw new IllegalArgumentException("管理员账号不存在");
        }
    }

    private void validateCodes(String table, List<String> codes, String label) {
        if (codes.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(codes.size(), "?"));
        String sql = "xw_permissions".equals(table)
                ? "SELECT COUNT(*) FROM xw_permissions WHERE code IN (" + placeholders + ")"
                : "SELECT COUNT(*) FROM " + table + " WHERE enabled = TRUE AND code IN (" + placeholders + ")";
        Long count = jdbc.queryForObject(sql, Long.class, codes.toArray());
        if (count == null || count != codes.size()) {
            throw new IllegalArgumentException(label + "包含不存在或已停用的编码");
        }
    }

    private static List<String> distinctCodes(List<String> values, int maxLength, String label) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String code = normalizeCode(value, maxLength, label);
            if (code != null) {
                result.add(code);
            }
        }
        return List.copyOf(result);
    }

    private static String normalizeCode(String value, int maxLength, String label) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(label + "过长");
        }
        return cleaned;
    }

    private static String clean(String value, int maxLength, String label) {
        String cleaned = blankToNull(value);
        if (cleaned != null && cleaned.length() > maxLength) {
            throw new IllegalArgumentException(label + "过长");
        }
        return cleaned;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public record ProfileInput(
            String employeeNo,
            String displayName,
            String departmentCode,
            String positionCode,
            String status,
            String notes) {}
}
