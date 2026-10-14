package com.xinghuo.wecom;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Syncs a role only when a verified WeCom title uniquely matches a built-in business position. */
@Service
public class XingweiWeComPositionRoleService {
    private final JdbcTemplate jdbc;

    public XingweiWeComPositionRoleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean needsRefresh(long adminUserId) {
        Long roles = jdbc.queryForObject(
                "SELECT COUNT(*) FROM xw_admin_roles WHERE admin_user_id = ?", Long.class, adminUserId);
        return autoManaged(adminUserId) || roles == null || roles == 0;
    }

    public boolean autoManaged(long adminUserId) {
        Long tracked = jdbc.queryForObject(
                "SELECT COUNT(*) FROM xw_wecom_auto_roles WHERE admin_user_id = ?", Long.class, adminUserId);
        return tracked != null && tracked > 0;
    }

    /** Never replaces roles an administrator assigned. Unknown, ambiguous or management titles grant nothing. */
    @Transactional
    public boolean sync(long adminUserId, String wecomPosition) {
        String title = wecomPosition == null ? "" : wecomPosition.trim();
        List<Tracked> previous = jdbc.query("""
                SELECT position_id, role_id FROM xw_wecom_auto_roles WHERE admin_user_id = ?
                """, (rs, n) -> new Tracked(rs.getLong("position_id"), rs.getLong("role_id")), adminUserId);
        if (previous.isEmpty() && roleCount(adminUserId) > 0) {
            return false;
        }
        Position chosen = resolveUniqueBusinessPosition(title);
        Tracked prior = previous.isEmpty() ? null : previous.getFirst();
        if (prior != null && chosen != null
                && prior.positionId() == chosen.positionId() && prior.roleId() == chosen.roleId()) {
            return false;
        }
        if (prior != null) {
            jdbc.update("DELETE FROM xw_admin_roles WHERE admin_user_id = ? AND role_id = ?",
                    adminUserId, prior.roleId());
            jdbc.update("DELETE FROM xw_wecom_auto_roles WHERE admin_user_id = ?", adminUserId);
            jdbc.update("""
                    UPDATE xw_admin_profiles SET position_id = NULL, department_id = NULL, updated_at = NOW(6)
                    WHERE admin_user_id = ? AND position_id = ?
                    """, adminUserId, prior.positionId());
        }
        if (chosen == null || roleCount(adminUserId) > 0) {
            return prior != null;
        }
        int active = jdbc.update("""
                UPDATE xw_admin_profiles SET position_id = ?, department_id = ?, updated_at = NOW(6)
                WHERE admin_user_id = ? AND status = 'ACTIVE'
                """, chosen.positionId(), chosen.departmentId(), adminUserId);
        if (active != 1) {
            return prior != null;
        }
        jdbc.update("""
                INSERT INTO xw_admin_roles (admin_user_id, role_id, is_primary, created_at)
                VALUES (?, ?, TRUE, NOW(6))
                """, adminUserId, chosen.roleId());
        jdbc.update("""
                INSERT INTO xw_wecom_auto_roles
                    (admin_user_id, position_id, role_id, wecom_position, updated_at)
                VALUES (?, ?, ?, ?, NOW(6))
                """, adminUserId, chosen.positionId(), chosen.roleId(), title);
        return true;
    }

    private long roleCount(long adminUserId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM xw_admin_roles WHERE admin_user_id = ?", Long.class, adminUserId);
        return count == null ? 0 : count;
    }

    private Position resolveUniqueBusinessPosition(String title) {
        if (title.isBlank() || title.length() > 80) {
            return null;
        }
        List<Candidate> candidates = jdbc.query("""
                SELECT p.id AS position_id, p.name AS position_name,
                       d.id AS department_id, d.code AS department_code
                FROM xw_positions p JOIN xw_departments d ON d.id = p.department_id
                WHERE p.name = ? AND p.built_in = TRUE
                  AND p.enabled = TRUE AND d.enabled = TRUE
                """, (rs, n) -> title.equals(rs.getString("position_name"))
                ? new Candidate(rs.getLong("position_id"), rs.getLong("department_id"),
                        rs.getString("department_code")) : null, title);
        candidates.removeIf(java.util.Objects::isNull);
        if (candidates.size() != 1) {
            return null;
        }
        Candidate candidate = candidates.getFirst();
        String roleCode = switch (candidate.departmentCode()) {
            case "02" -> "XW_OPERATIONS";
            case "03" -> "XW_CUSTOMER_ORDER";
            case "04" -> "XW_GAME_BUSINESS";
            case "05" -> "XW_TECH_PRODUCT";
            case "06" -> "XW_MARKET_CHANNEL";
            case "07" -> "XW_GENERAL_SUPPORT";
            default -> null;
        };
        if (roleCode == null) {
            return null;
        }
        List<Long> roleIds = jdbc.queryForList(
                "SELECT id FROM xw_rbac_roles WHERE code = ? AND enabled = TRUE AND data_scope = 'DEPARTMENT'",
                Long.class, roleCode);
        if (roleIds.size() != 1) {
            return null;
        }
        return new Position(candidate.positionId(), candidate.departmentId(), roleIds.getFirst());
    }

    private record Candidate(long positionId, long departmentId, String departmentCode) {}
    private record Position(long positionId, long departmentId, long roleId) {}
    private record Tracked(long positionId, long roleId) {}
}
