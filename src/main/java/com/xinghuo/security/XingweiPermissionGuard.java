package com.xinghuo.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * 星维 V2B 混合权限守卫。
 *
 * <p>安全策略：legacy ADMIN 始终全权；未配置影子角色的 legacy OPERATOR
 * 仅获得兼容的查看权限；一旦明确分配影子角色，则按 xw_role_permissions 精确授权。
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
        return snapshot.allowAll() || snapshot.permissionCodes().contains(permissionCode);
    }

    public AccessSnapshot resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AccessSnapshot("DENIED", false, List.of(), List.of());
        }
        if (hasAuthority(authentication, "ROLE_ADMIN")) {
            return new AccessSnapshot("LEGACY_ADMIN_OVERRIDE", true, allPermissionCodes(), roleCodes(authentication.getName()));
        }
        if (!hasAuthority(authentication, "ROLE_OPERATOR")) {
            return new AccessSnapshot("DENIED", false, List.of(), List.of());
        }

        Long adminUserId = adminUserId(authentication.getName());
        if (adminUserId == null) {
            return new AccessSnapshot("DENIED", false, List.of(), List.of());
        }

        List<String> roles = roleCodes(adminUserId);
        if (roles.isEmpty()) {
            List<String> compatibility = new ArrayList<>(LEGACY_OPERATOR_VIEW_PERMISSIONS);
            compatibility.sort(String::compareTo);
            return new AccessSnapshot("LEGACY_OPERATOR_COMPAT", false, List.copyOf(compatibility), List.of());
        }

        return new AccessSnapshot("SHADOW_RBAC", false, permissionCodes(adminUserId), roles);
    }

    public Map<String, Object> asMap(Authentication authentication) {
        AccessSnapshot snapshot = resolve(authentication);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", snapshot.mode());
        result.put("allowAll", snapshot.allowAll());
        result.put("permissionCodes", snapshot.permissionCodes());
        result.put("roleCodes", snapshot.roleCodes());
        result.put("legacyAdminOverride", "LEGACY_ADMIN_OVERRIDE".equals(snapshot.mode()));
        result.put("compatibilityMode", "LEGACY_OPERATOR_COMPAT".equals(snapshot.mode()));
        return result;
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

    private List<String> roleCodes(String username) {
        Long adminUserId = adminUserId(username);
        return adminUserId == null ? List.of() : roleCodes(adminUserId);
    }

    private List<String> roleCodes(long adminUserId) {
        return jdbc.query(
                """
                SELECT r.code
                FROM xw_admin_roles ar
                JOIN xw_rbac_roles r ON r.id = ar.role_id
                WHERE ar.admin_user_id = ? AND r.enabled = TRUE
                ORDER BY ar.is_primary DESC, r.sort_order, r.id
                """,
                (rs, rowNum) -> rs.getString("code"),
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

    private static boolean hasAuthority(Authentication authentication, String authority) {
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
            if (authority.equals(grantedAuthority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public record AccessSnapshot(
            String mode,
            boolean allowAll,
            List<String> permissionCodes,
            List<String> roleCodes) {}
}
