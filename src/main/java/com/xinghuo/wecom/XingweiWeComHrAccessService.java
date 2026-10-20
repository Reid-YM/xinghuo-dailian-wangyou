package com.xinghuo.wecom;

import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Local revocation is durable even when the remote disable result is uncertain. */
@Service
public class XingweiWeComHrAccessService {
    private final JdbcTemplate jdbc;
    private final AuditService audits;

    public XingweiWeComHrAccessService(JdbcTemplate jdbc, AuditService audits) {
        this.jdbc = jdbc;
        this.audits = audits;
    }

    public void checkDisable(String userId, String operator) { checkedBindings(userId, operator, false); }

    @Transactional
    public void beginDisable(String userId, String operator, HttpServletRequest request) {
        List<Long> ids = checkedBindings(userId, operator, true);
        for (Long id : ids) {
            jdbc.update("UPDATE admin_users SET enabled=FALSE, auth_version=auth_version+1, updated_at=NOW(6) WHERE id=?", id);
            jdbc.update("UPDATE xw_admin_profiles SET status='INACTIVE', updated_at=NOW(6) WHERE admin_user_id=?", id);
        }
        audits.log("XW_WECOM_DISABLE_REQUEST", "WECOM_MEMBER", userId, operator,
                null, Map.of("status", "后台访问已停用"), "停用成员并撤销后台访问", request);
    }

    private List<Long> checkedBindings(String userId, String operator, boolean lock) {
        return jdbc.query("""
                SELECT u.id,u.username,u.role,p.wecom_user_id FROM admin_users u
                JOIN xw_admin_profiles p ON p.admin_user_id=u.id
                WHERE p.wecom_user_id=?
                """ + (lock ? " FOR UPDATE" : ""), (rs, row) -> {
            if (!userId.equals(rs.getString("wecom_user_id"))) return null;
            if (operator.equals(rs.getString("username")) || "ADMIN".equals(rs.getString("role")))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "不能在通讯录停用本人或系统管理员，请先核对账号交接");
            return rs.getLong("id");
        }, userId).stream().filter(java.util.Objects::nonNull).toList();
    }
}
