package com.xinghuo.wecom;

import com.xinghuo.service.AdminUserService;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Explicit administrator-managed binding. OAuth never creates or binds an account. */
@Service
public class XingweiWeComBindingService {
    private final JdbcTemplate jdbc;
    private final AdminUserService users;

    public XingweiWeComBindingService(JdbcTemplate jdbc, AdminUserService users) {
        this.jdbc = jdbc;
        this.users = users;
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
