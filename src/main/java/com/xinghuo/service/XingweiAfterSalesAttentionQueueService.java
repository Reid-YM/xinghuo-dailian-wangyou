package com.xinghuo.service;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.XingweiAfterSalesCaseService.CaseRecord;
import com.xinghuo.service.XingweiAfterSalesCaseService.QueueItem;
import com.xinghuo.service.XingweiAfterSalesCaseService.QueueSnapshot;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only filtered queue over the same attention semantics as the V4J aging summary. */
@Service
public class XingweiAfterSalesAttentionQueueService {
    private static final int QUEUE_PAGE_SIZE = 50;
    private static final int MAX_QUEUE_PAGE = 200;
    private static final long H24_MS = Duration.ofHours(24).toMillis();
    private static final long H72_MS = Duration.ofHours(72).toMillis();

    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard permissions;

    public XingweiAfterSalesAttentionQueueService(JdbcTemplate jdbc, XingweiPermissionGuard permissions) {
        this.jdbc = jdbc;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public QueueSnapshot queue(String age, String status, String category, String followup, String query, int page,
            Authentication authentication) {
        return queue(age, status, category, followup, query, "NEWEST_FIRST", page, authentication);
    }

    @Transactional(readOnly = true)
    public QueueSnapshot queue(String age, String status, String category, String followup, String query, String sort,
            int page, Authentication authentication) {
        if (!permissions.has(authentication, "order:view")) {
            throw new AccessDeniedException("缺少订单查看权限");
        }
        if (page < 0 || page > MAX_QUEUE_PAGE) throw new IllegalArgumentException("页码超出范围");

        String normalizedAge = age == null ? "" : age.trim();
        if (!List.of("ATTENTION", "WITHIN_24H", "FROM_24_TO_72H", "OVER_72H").contains(normalizedAge)) {
            throw new IllegalArgumentException("待跟进时效无效");
        }
        String normalizedStatus = status == null ? "" : status.trim();
        if (!normalizedStatus.isEmpty() && !List.of("OPEN", "IN_PROGRESS", "RESOLVED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("处理状态无效");
        }
        String normalizedCategory = category == null ? "" : category.trim();
        if (!normalizedCategory.isEmpty()
                && !List.of("DELIVERY", "CANCELLATION", "REFUND_REVIEW", "OTHER").contains(normalizedCategory)) {
            throw new IllegalArgumentException("问题类型无效");
        }
        String normalizedFollowup = followup == null ? "" : followup.trim();
        if (!normalizedFollowup.isEmpty() && !"PENDING".equals(normalizedFollowup)) {
            throw new IllegalArgumentException("客户补充筛选无效");
        }
        String normalizedSort = sort == null || sort.isBlank() ? "NEWEST_FIRST" : sort.trim();
        if (!List.of("NEWEST_FIRST", "OLDEST_FIRST").contains(normalizedSort)) {
            throw new IllegalArgumentException("待跟进排序无效");
        }
        String keyword = query == null ? "" : query.trim();
        if (keyword.length() > 80) throw new IllegalArgumentException("搜索内容不能超过 80 字");

        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        StringBuilder where = new StringBuilder(scopeSql(access));
        List<Object> innerArgs = scopeArgs(access);
        if (!normalizedStatus.isEmpty()) {
            where.append(" AND COALESCE(s.status, 'OPEN') = ?");
            innerArgs.add(normalizedStatus);
        }
        if (!normalizedCategory.isEmpty()) {
            where.append(" AND r.category = ?");
            innerArgs.add(normalizedCategory);
        }
        if ("PENDING".equals(normalizedFollowup)) {
            where.append(" AND f.latest_followup_id > COALESCE(s.handled_through_followup_id, 0)");
        }
        if (!keyword.isEmpty()) {
            where.append(" AND (LOCATE(?, COALESCE(o.business_no, o.order_no)) > 0")
                    .append(" OR LOCATE(?, o.order_no) > 0 OR LOCATE(?, r.content) > 0)");
            innerArgs.add(keyword);
            innerArgs.add(keyword);
            innerArgs.add(keyword);
        }

        String inner = """
                SELECT COALESCE(o.business_no, o.order_no) AS business_no,
                       r.id, r.order_no, r.category, r.category_label, r.content,
                       r.submitted_at_ms, s.status, s.handling_note,
                       s.handled_by_admin_user_id, s.updated_at, s.version,
                       f.latest_followup_at_ms, f.latest_followup_id,
                       COALESCE(s.handled_through_followup_id, 0) AS handled_through_followup_id,
                       CASE
                         WHEN f.latest_followup_id > COALESCE(s.handled_through_followup_id, 0)
                           THEN f.latest_followup_at_ms
                         WHEN COALESCE(s.status, 'OPEN') = 'OPEN'
                           THEN r.submitted_at_ms
                         ELSE NULL
                       END AS attention_at_ms
                  FROM customer_order_after_sales_records r
                  JOIN customer_orders o ON o.order_no = r.order_no
                  LEFT JOIN xw_business_ownership own
                         ON own.business_no = COALESCE(o.business_no, o.order_no)
                  LEFT JOIN xw_departments d ON d.id = own.department_id
                  LEFT JOIN customer_order_after_sales_case_state s ON s.record_id = r.id
                  LEFT JOIN (
                        SELECT record_id, MAX(created_at_ms) AS latest_followup_at_ms,
                               MAX(id) AS latest_followup_id
                          FROM customer_order_after_sales_case_followups
                         GROUP BY record_id
                  ) f ON f.record_id = r.id
                 WHERE %s
                """.formatted(where);

        long now = System.currentTimeMillis();
        long cutoff24 = now - H24_MS;
        long cutoff72 = now - H72_MS;
        String ageWhere = "attention_at_ms IS NOT NULL";
        List<Object> ageArgs = new ArrayList<>();
        if ("WITHIN_24H".equals(normalizedAge)) {
            ageWhere += " AND attention_at_ms > ?";
            ageArgs.add(cutoff24);
        } else if ("FROM_24_TO_72H".equals(normalizedAge)) {
            ageWhere += " AND attention_at_ms <= ? AND attention_at_ms > ?";
            ageArgs.add(cutoff24);
            ageArgs.add(cutoff72);
        } else if ("OVER_72H".equals(normalizedAge)) {
            ageWhere += " AND attention_at_ms <= ?";
            ageArgs.add(cutoff72);
        }

        List<Object> countArgs = new ArrayList<>(innerArgs);
        countArgs.addAll(ageArgs);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + inner + ") attention WHERE " + ageWhere,
                Long.class, countArgs.toArray());

        List<Object> itemArgs = new ArrayList<>(countArgs);
        itemArgs.add(QUEUE_PAGE_SIZE);
        itemArgs.add(page * QUEUE_PAGE_SIZE);
        String orderBy = "OLDEST_FIRST".equals(normalizedSort)
                ? "attention_at_ms ASC, id ASC"
                : "attention_at_ms DESC, id DESC";
        String sql = """
                SELECT business_no, id, order_no, category, category_label, content,
                       submitted_at_ms, status, handling_note, handled_by_admin_user_id,
                       updated_at, version, latest_followup_at_ms, latest_followup_id,
                       handled_through_followup_id
                  FROM (%s) attention
                 WHERE %s
                 ORDER BY %s
                 LIMIT ? OFFSET ?
                """.formatted(inner, ageWhere, orderBy);
        List<QueueItem> items = jdbc.query(sql, (rs, rowNum) -> {
            Object handler = rs.getObject("handled_by_admin_user_id");
            Timestamp updated = rs.getTimestamp("updated_at");
            Object version = rs.getObject("version");
            Object latest = rs.getObject("latest_followup_at_ms");
            CaseRecord record = new CaseRecord(
                    rs.getLong("id"), rs.getString("order_no"),
                    rs.getString("category"), rs.getString("category_label"),
                    rs.getString("content"), rs.getLong("submitted_at_ms"),
                    rs.getString("status") == null ? "OPEN" : rs.getString("status"),
                    rs.getString("handling_note"),
                    handler == null ? null : ((Number) handler).longValue(),
                    updated == null ? null : updated.toInstant(),
                    version == null ? 0 : ((Number) version).longValue());
            return new QueueItem(rs.getString("business_no"), record,
                    latest == null ? null : ((Number) latest).longValue(),
                    rs.getLong("latest_followup_id") > rs.getLong("handled_through_followup_id"));
        }, itemArgs.toArray());

        long count = total == null ? 0L : total;
        return new QueueSnapshot(items, count, page, QUEUE_PAGE_SIZE, normalizedStatus,
                normalizedCategory, normalizedFollowup, keyword,
                page < MAX_QUEUE_PAGE && ((long) page + 1) * QUEUE_PAGE_SIZE < count);
    }

    private List<Object> scopeArgs(XingweiPermissionGuard.AccessSnapshot access) {
        List<Object> args = new ArrayList<>();
        if ("DEPARTMENT".equals(access.dataScope()) && !access.allowAll() && !"DENIED".equals(access.mode())
                && access.departmentCode() != null) args.add(access.departmentCode());
        if ("SELF".equals(access.dataScope()) && !access.allowAll() && !"DENIED".equals(access.mode())
                && access.adminUserId() != null) args.add(access.adminUserId());
        return args;
    }

    private String scopeSql(XingweiPermissionGuard.AccessSnapshot access) {
        if ("DENIED".equals(access.mode())) return "1 = 0";
        if (access.allowAll() || "ALL".equals(access.dataScope())) return "1 = 1";
        if ("DEPARTMENT".equals(access.dataScope()) && access.departmentCode() != null) return "d.code = ?";
        if ("SELF".equals(access.dataScope()) && access.adminUserId() != null) return "own.owner_admin_user_id = ?";
        return "1 = 0";
    }
}
