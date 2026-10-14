package com.xinghuo.service;

import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Staff-only workflow over the immutable customer after-sales submission ledger. */
@Service
public class XingweiAfterSalesCaseService {
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard permissions;
    private final XingweiBusinessScopeService businessScope;
    private final AuditService audit;

    public XingweiAfterSalesCaseService(JdbcTemplate jdbc, XingweiPermissionGuard permissions,
            XingweiBusinessScopeService businessScope, AuditService audit) {
        this.jdbc = jdbc;
        this.permissions = permissions;
        this.businessScope = businessScope;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CaseRecord> records(String businessNo, Authentication authentication) {
        String orderNo = orderNo(businessNo, authentication, "order:view");
        return jdbc.query("""
                SELECT r.id, r.order_no, r.category, r.category_label, r.content,
                       r.submitted_at_ms, s.status, s.handling_note,
                       s.handled_by_admin_user_id, s.updated_at, s.version
                  FROM customer_order_after_sales_records r
                  LEFT JOIN customer_order_after_sales_case_state s ON s.record_id = r.id
                 WHERE r.order_no = ?
                 ORDER BY r.submitted_at_ms, r.id
                """, this::map, orderNo);
    }

    @Transactional
    public CaseRecord update(String businessNo, long recordId, String status, String handlingNote,
            long expectedVersion, Authentication authentication, HttpServletRequest request) {
        String orderNo = orderNo(businessNo, authentication, "order:manage");
        Long actorId = permissions.resolve(authentication).adminUserId();
        if (actorId == null) throw new AccessDeniedException("当前账号没有员工身份");
        if (recordId <= 0 || expectedVersion < 0) throw new IllegalArgumentException("记录或版本号无效");
        String next = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"IN_PROGRESS".equals(next) && !"RESOLVED".equals(next)) {
            throw new IllegalArgumentException("处理状态只允许 IN_PROGRESS 或 RESOLVED");
        }
        String note = handlingNote == null ? "" : handlingNote.trim();
        if (note.isEmpty() || note.length() > 600) {
            throw new IllegalArgumentException("处理说明须为 1 至 600 字");
        }
        CaseRecord before = record(orderNo, recordId);
        if (before == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "售后记录不存在");
        if (before.version() != expectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "售后记录已由其他人更新，请刷新后重试");
        }
        if (next.equals(before.status())) {
            throw new IllegalArgumentException("处理状态未发生变化");
        }
        if (before.version() == 0) {
            try {
                jdbc.update("""
                        INSERT INTO customer_order_after_sales_case_state
                            (record_id, status, handling_note, handled_by_admin_user_id, updated_at, version)
                        VALUES (?, ?, ?, ?, NOW(6), 1)
                        """, recordId, next, note, actorId);
            } catch (DuplicateKeyException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "售后记录已由其他人更新，请刷新后重试", ex);
            }
        } else {
            int updated = jdbc.update("""
                    UPDATE customer_order_after_sales_case_state
                       SET status = ?, handling_note = ?, handled_by_admin_user_id = ?,
                           updated_at = NOW(6), version = version + 1
                     WHERE record_id = ? AND version = ?
                    """, next, note, actorId, recordId, expectedVersion);
            if (updated != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "售后记录已由其他人更新，请刷新后重试");
            }
        }
        CaseRecord after = record(orderNo, recordId);
        audit.log("XW_AFTER_SALES_CASE_UPDATE", "AFTER_SALES_RECORD", String.valueOf(recordId),
                authentication.getName(), before, after, note, request);
        return after;
    }

    private String orderNo(String businessNo, Authentication authentication, String permission) {
        if (!permissions.has(authentication, permission)) throw new AccessDeniedException("缺少订单权限");
        if (businessNo == null || businessNo.isBlank()) throw new IllegalArgumentException("业务编号不能为空");
        String normalized = businessNo.trim();
        List<String> matches = jdbc.query("""
                SELECT order_no FROM customer_orders
                 WHERE COALESCE(business_no, order_no) = ?
                """, (rs, rowNum) -> rs.getString(1), normalized);
        if (matches.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        if (!businessScope.canAccessBusiness(authentication, normalized)) {
            throw new AccessDeniedException("当前订单不在你的数据范围");
        }
        return matches.getFirst();
    }

    private CaseRecord record(String orderNo, long recordId) {
        return jdbc.query("""
                SELECT r.id, r.order_no, r.category, r.category_label, r.content,
                       r.submitted_at_ms, s.status, s.handling_note,
                       s.handled_by_admin_user_id, s.updated_at, s.version
                  FROM customer_order_after_sales_records r
                  LEFT JOIN customer_order_after_sales_case_state s ON s.record_id = r.id
                 WHERE r.order_no = ? AND r.id = ?
                """, this::map, orderNo, recordId).stream().findFirst().orElse(null);
    }

    private CaseRecord map(ResultSet rs, int rowNum) throws SQLException {
        Object handler = rs.getObject("handled_by_admin_user_id");
        Timestamp updated = rs.getTimestamp("updated_at");
        Object version = rs.getObject("version");
        return new CaseRecord(rs.getLong("id"), rs.getString("order_no"),
                rs.getString("category"), rs.getString("category_label"),
                rs.getString("content"), rs.getLong("submitted_at_ms"),
                rs.getString("status") == null ? "OPEN" : rs.getString("status"),
                rs.getString("handling_note"),
                handler == null ? null : ((Number) handler).longValue(),
                updated == null ? null : updated.toInstant(),
                version == null ? 0 : ((Number) version).longValue());
    }

    public record CaseRecord(long recordId, String orderNo, String category, String categoryLabel,
            String content, long submittedAtMs, String status, String handlingNote,
            Long handledByAdminUserId, java.time.Instant updatedAt, long version) {}
}
