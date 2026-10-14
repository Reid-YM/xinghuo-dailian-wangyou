package com.xinghuo.service;

import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
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
    private static final int QUEUE_PAGE_SIZE = 50;
    private static final int MAX_QUEUE_PAGE = 200;
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

    @Transactional(readOnly = true)
    public QueueSnapshot queue(String status, String category, String query, int page,
            Authentication authentication) {
        if (!permissions.has(authentication, "order:view")) {
            throw new AccessDeniedException("缺少订单查看权限");
        }
        if (page < 0 || page > MAX_QUEUE_PAGE) throw new IllegalArgumentException("页码超出范围");
        String normalizedStatus = status == null ? "" : status.trim();
        if (!normalizedStatus.isEmpty() && !List.of("OPEN", "IN_PROGRESS", "RESOLVED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("处理状态无效");
        }
        String normalizedCategory = category == null ? "" : category.trim();
        if (!normalizedCategory.isEmpty()
                && !List.of("DELIVERY", "CANCELLATION", "REFUND_REVIEW", "OTHER").contains(normalizedCategory)) {
            throw new IllegalArgumentException("问题类型无效");
        }
        String keyword = query == null ? "" : query.trim();
        if (keyword.length() > 80) throw new IllegalArgumentException("搜索内容不能超过 80 字");

        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        StringBuilder where = new StringBuilder(scopeSql(access));
        List<Object> args = new ArrayList<>();
        if ("DEPARTMENT".equals(access.dataScope()) && !access.allowAll() && !"DENIED".equals(access.mode())
                && access.departmentCode() != null) args.add(access.departmentCode());
        if ("SELF".equals(access.dataScope()) && !access.allowAll() && !"DENIED".equals(access.mode())
                && access.adminUserId() != null) args.add(access.adminUserId());
        if (!normalizedStatus.isEmpty()) {
            where.append(" AND COALESCE(s.status, 'OPEN') = ?");
            args.add(normalizedStatus);
        }
        if (!normalizedCategory.isEmpty()) {
            where.append(" AND r.category = ?");
            args.add(normalizedCategory);
        }
        if (!keyword.isEmpty()) {
            where.append(" AND (LOCATE(?, COALESCE(o.business_no, o.order_no)) > 0")
                    .append(" OR LOCATE(?, o.order_no) > 0 OR LOCATE(?, r.content) > 0)");
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        String from = """
                FROM customer_order_after_sales_records r
                JOIN customer_orders o ON o.order_no = r.order_no
                LEFT JOIN xw_business_ownership own
                       ON own.business_no = COALESCE(o.business_no, o.order_no)
                LEFT JOIN xw_departments d ON d.id = own.department_id
                LEFT JOIN customer_order_after_sales_case_state s ON s.record_id = r.id
                LEFT JOIN (
                    SELECT record_id, MAX(created_at_ms) AS latest_followup_at_ms
                      FROM customer_order_after_sales_case_followups GROUP BY record_id
                ) f ON f.record_id = r.id
                WHERE %s
                """.formatted(where);
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + from, Long.class, args.toArray());
        String sql = """
                SELECT COALESCE(o.business_no, o.order_no) AS business_no,
                       r.id, r.order_no, r.category, r.category_label, r.content,
                       r.submitted_at_ms, s.status, s.handling_note,
                       s.handled_by_admin_user_id, s.updated_at, s.version,
                       f.latest_followup_at_ms
                %s
                ORDER BY GREATEST(r.submitted_at_ms, COALESCE(f.latest_followup_at_ms, 0)) DESC, r.id DESC
                LIMIT ? OFFSET ?
                """.formatted(from);
        args.add(QUEUE_PAGE_SIZE);
        args.add(page * QUEUE_PAGE_SIZE);
        List<QueueItem> items = jdbc.query(sql,
                (rs, rowNum) -> {
                    Object latest = rs.getObject("latest_followup_at_ms");
                    return new QueueItem(rs.getString("business_no"), map(rs, rowNum),
                            latest == null ? null : ((Number) latest).longValue());
                }, args.toArray());
        long count = total == null ? 0L : total;
        return new QueueSnapshot(items, count, page, QUEUE_PAGE_SIZE, normalizedStatus,
                normalizedCategory, keyword,
                page < MAX_QUEUE_PAGE && ((long) page + 1) * QUEUE_PAGE_SIZE < count);
    }

    private String scopeSql(XingweiPermissionGuard.AccessSnapshot access) {
        if ("DENIED".equals(access.mode())) return "1 = 0";
        if (access.allowAll() || "ALL".equals(access.dataScope())) return "1 = 1";
        if ("DEPARTMENT".equals(access.dataScope()) && access.departmentCode() != null) return "d.code = ?";
        if ("SELF".equals(access.dataScope()) && access.adminUserId() != null) {
            return "own.owner_admin_user_id = ?";
        }
        return "1 = 0";
    }

    @Transactional
    public CaseRecord update(String businessNo, long recordId, String status, String handlingNote,
            long expectedVersion, Authentication authentication, HttpServletRequest request) {
        return update(businessNo, recordId, status, handlingNote, null,
                expectedVersion, authentication, request);
    }

    @Transactional
    public CaseRecord update(String businessNo, long recordId, String status, String handlingNote,
            String customerReply, long expectedVersion, Authentication authentication, HttpServletRequest request) {
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
        String reply = customerReply == null ? null : customerReply.trim();
        if (reply != null && (reply.isEmpty() || reply.length() > 600)) {
            throw new IllegalArgumentException("客户回复须为 1 至 600 字，或不填写");
        }
        CaseRecord before = record(orderNo, recordId);
        if (before == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "售后记录不存在");
        if (before.version() != expectedVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "售后记录已由其他人更新，请刷新后重试");
        }
        if (next.equals(before.status()) && reply == null) {
            throw new IllegalArgumentException("状态不变时须填写客户回复");
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
        jdbc.update("""
                INSERT INTO customer_order_after_sales_case_events
                    (record_id, from_status, to_status, handling_note, customer_reply,
                     handled_by_admin_user_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW(6))
                """, recordId, before.status(), next, note, reply, actorId);
        CaseRecord after = record(orderNo, recordId);
        audit.log("XW_AFTER_SALES_CASE_UPDATE", "AFTER_SALES_RECORD", String.valueOf(recordId),
                authentication.getName(), before, after, note, request);
        return after;
    }

    @Transactional(readOnly = true)
    public CaseHistory history(String businessNo, long recordId, Authentication authentication) {
        String orderNo = orderNo(businessNo, authentication, "order:view");
        if (recordId <= 0) throw new IllegalArgumentException("记录编号无效");
        CaseRecord current = record(orderNo, recordId);
        if (current == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "售后记录不存在");
        List<CaseEvent> events = jdbc.query("""
                SELECT e.id, e.from_status, e.to_status, e.handling_note,
                       e.customer_reply, e.handled_by_admin_user_id, e.created_at
                  FROM customer_order_after_sales_case_events e
                 WHERE e.record_id = ?
                 ORDER BY e.id
                """, (rs, rowNum) -> new CaseEvent(rs.getLong("id"),
                        rs.getString("from_status"), rs.getString("to_status"),
                        rs.getString("handling_note"), rs.getString("customer_reply"),
                        rs.getLong("handled_by_admin_user_id"),
                        rs.getTimestamp("created_at").toInstant()), recordId);
        List<CustomerFollowup> customerFollowups = jdbc.query("""
                SELECT id, content, created_at_ms
                  FROM customer_order_after_sales_case_followups
                 WHERE record_id = ? ORDER BY id
                """, (rs, rowNum) -> new CustomerFollowup(rs.getLong("id"),
                        rs.getString("content"), java.time.Instant.ofEpochMilli(rs.getLong("created_at_ms"))), recordId);
        return new CaseHistory(current, events, customerFollowups);
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

    public record CaseEvent(long eventId, String fromStatus, String toStatus, String handlingNote,
            String customerReply, long handledByAdminUserId, java.time.Instant createdAt) {}

    public record CustomerFollowup(long followupId, String content, java.time.Instant createdAt) {}

    public record CaseHistory(CaseRecord current, List<CaseEvent> events,
                              List<CustomerFollowup> customerFollowups) {}

    public record QueueItem(String businessNo, CaseRecord record, Long latestCustomerFollowupAtMs) {}

    public record QueueSnapshot(List<QueueItem> items, long totalCount, int page, int pageSize,
            String status, String category, String query, boolean hasNext) {}
}
