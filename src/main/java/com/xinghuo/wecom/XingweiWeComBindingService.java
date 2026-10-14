package com.xinghuo.wecom;

import com.xinghuo.service.AdminUserService;
import java.sql.Statement;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Explicit bindings and least-privilege enrollment for verified WeCom members. */
@Service
public class XingweiWeComBindingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;
    private final AdminUserService users;
    private final PasswordEncoder passwords;

    public XingweiWeComBindingService(JdbcTemplate jdbc, AdminUserService users, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.users = users;
        this.passwords = passwords;
    }

    public Long boundAdminUserId(String wecomUserId) {
        List<Long> ids = jdbc.query(
                "SELECT admin_user_id, wecom_user_id FROM xw_admin_profiles WHERE wecom_user_id = ?",
                (rs, row) -> wecomUserId.equals(rs.getString("wecom_user_id"))
                        ? rs.getLong("admin_user_id") : null, wecomUserId);
        ids.removeIf(java.util.Objects::isNull);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    /** New members receive an unguessable password and no Xingwei role or business permissions. */
    @Transactional
    public long registerPending(String wecomUserId, String displayName) {
        if (wecomUserId == null || wecomUserId.isBlank() || wecomUserId.length() > 64
                || !wecomUserId.matches("[A-Za-z0-9_@.\\-]+")) {
            throw new IllegalArgumentException("企业微信 UserID 格式不合法");
        }
        if (boundAdminUserId(wecomUserId) != null) {
            throw new IllegalArgumentException("企业微信成员已登记");
        }
        String name = displayName == null || displayName.isBlank()
                ? wecomUserId : displayName.trim();
        if (name.length() > 80) {
            name = name.substring(0, 80);
        }
        byte[] randomPassword = new byte[32];
        RANDOM.nextBytes(randomPassword);
        String passwordHash = passwords.encode(Base64.getUrlEncoder().withoutPadding()
                .encodeToString(randomPassword));
        String username = "wecom_" + UUID.randomUUID().toString().replace("-", "");
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO admin_users (username, password_hash, role, enabled, created_at, updated_at)
                    VALUES (?, ?, 'OPERATOR', TRUE, NOW(6), NOW(6))
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            return statement;
        }, keys);
        long adminUserId = keys.getKey().longValue();
        jdbc.update("""
                INSERT INTO xw_admin_profiles
                    (admin_user_id, display_name, status, wecom_user_id, created_at, updated_at)
                VALUES (?, ?, 'ACTIVE', ?, NOW(6), NOW(6))
                """, adminUserId, name, wecomUserId);
        return adminUserId;
    }

    public UserDetails loginUser(String wecomUserId) {
        List<String> usernames = jdbc.queryForList("""
                SELECT u.username
                FROM xw_admin_profiles p
                JOIN admin_users u ON u.id = p.admin_user_id
                WHERE BINARY p.wecom_user_id = BINARY ?
                  AND p.status = 'ACTIVE' AND u.enabled = TRUE
                  AND u.role IN ('ADMIN', 'OPERATOR')
                  AND (u.role = 'ADMIN' OR EXISTS (
                      SELECT 1 FROM xw_admin_roles ar
                      JOIN xw_rbac_roles r ON r.id = ar.role_id
                      WHERE ar.admin_user_id = u.id AND r.enabled = TRUE))
                """, String.class, wecomUserId);
        if (usernames.size() != 1) {
            throw new IllegalArgumentException("企业微信成员未绑定可用的星维账号");
        }
        UserDetails details = users.loadUserByUsername(usernames.getFirst());
        if (!details.isEnabled()) {
            throw new IllegalArgumentException("星维账号已停用");
        }
        return details;
    }

    public boolean activeSsoUser(String username, String wecomUserId, String role) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM admin_users u
                JOIN xw_admin_profiles p ON p.admin_user_id = u.id
                WHERE u.username = ? AND u.enabled = TRUE AND p.status = 'ACTIVE'
                  AND BINARY p.wecom_user_id = BINARY ? AND u.role = ?
                  AND u.role IN ('ADMIN', 'OPERATOR')
                  AND (u.role = 'ADMIN' OR EXISTS (
                      SELECT 1 FROM xw_admin_roles ar
                      JOIN xw_rbac_roles r ON r.id = ar.role_id
                      WHERE ar.admin_user_id = u.id AND r.enabled = TRUE))
                  AND NOT EXISTS (
                      SELECT 1 FROM xw_wecom_auto_roles tracked
                      LEFT JOIN xw_rbac_roles auto_role ON auto_role.id = tracked.role_id
                      WHERE tracked.admin_user_id = u.id
                        AND (auto_role.id IS NULL OR auto_role.enabled = FALSE
                             OR auto_role.data_scope <> 'DEPARTMENT'))
                """, Long.class, username, wecomUserId, role);
        return count != null && count == 1;
    }

    public void bind(long adminUserId, String wecomUserId) {
        String normalized = wecomUserId == null ? null : wecomUserId.trim();
        if (normalized != null && (normalized.isBlank() || normalized.length() > 64
                || !normalized.matches("[A-Za-z0-9_@.\\-]+"))) {
            throw new IllegalArgumentException("企业微信 UserID 格式不合法");
        }
        try {
            int changed = jdbc.update("""
                    UPDATE xw_admin_profiles SET wecom_user_id = ?, updated_at = NOW(6)
                    WHERE admin_user_id = ?
                    """, normalized, adminUserId);
            if (changed != 1) {
                bindingFor(adminUserId);
            }
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("企业微信 UserID 已绑定其他账号");
        }
    }

    public String bindingFor(long adminUserId) {
        List<String> values = jdbc.queryForList(
                "SELECT wecom_user_id FROM xw_admin_profiles WHERE admin_user_id = ?",
                String.class, adminUserId);
        if (values.size() != 1) {
            throw new IllegalArgumentException("星维员工档案不存在");
        }
        return values.getFirst();
    }
}
