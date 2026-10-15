package com.xinghuo.service;

import com.xinghuo.security.XingweiPermissionGuard;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only aging distribution for after-sales records that currently need staff attention. */
@Service
public class XingweiAfterSalesAttentionAgeSummaryService {
    private static final long H24_MS = Duration.ofHours(24).toMillis();
    private static final long H72_MS = Duration.ofHours(72).toMillis();

    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard permissions;

    public XingweiAfterSalesAttentionAgeSummaryService(JdbcTemplate jdbc, XingweiPermissionGuard permissions) {
        this.jdbc = jdbc;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public AttentionAgeSummary summary(Authentication authentication) {
        if (!permissions.has(authentication, "order:view")) {
            throw new AccessDeniedException("缺少订单查看权限");
        }
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        long now = System.currentTimeMillis();
        long cutoff24 = now - H24_MS;
        long cutoff72 = now - H72_MS;
        List<Object> args = new ArrayList<>();
        args.add(cutoff24);
        args.add(cutoff24);
        args.add(cutoff72);
        args.add(cutoff72);
        args.addAll(scopeArgs(access));

        return jdbc.queryForObject("""
                SELECT COUNT(*) AS attention_count,
                       COALESCE(SUM(CASE WHEN attention_at_ms > ? THEN 1 ELSE 0 END), 0) AS within_24h_count,
                       COALESCE(SUM(CASE WHEN attention_at_ms <= ? AND attention_at_ms > ? THEN 1 ELSE 0 END), 0)
                           AS from_24_to_72h_count,
                       COALESCE(SUM(CASE WHEN attention_at_ms <= ? THEN 1 ELSE 0 END), 0) AS over_72h_count
                  FROM (
                        SELECT CASE
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
                       ) attention
                 WHERE attention_at_ms IS NOT NULL
                """.formatted(scopeSql(access)),
                (rs, rowNum) -> new AttentionAgeSummary(
                        rs.getLong("attention_count"),
                        rs.getLong("within_24h_count"),
                        rs.getLong("from_24_to_72h_count"),
                        rs.getLong("over_72h_count")),
                args.toArray());
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
        if ("SELF".equals(access.dataScope()) && access.adminUserId() != null) {
            return "own.owner_admin_user_id = ?";
        }
        return "1 = 0";
    }

    public record AttentionAgeSummary(long attentionCount, long within24hCount,
                                      long from24To72hCount, long over72hCount) {}
}
