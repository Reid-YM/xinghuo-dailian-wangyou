package com.xinghuo.service;

import com.xinghuo.security.XingweiPermissionGuard;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only issue-type distribution over the same permission scope as the after-sales queue. */
@Service
public class XingweiAfterSalesCategorySummaryService {
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard permissions;

    public XingweiAfterSalesCategorySummaryService(JdbcTemplate jdbc, XingweiPermissionGuard permissions) {
        this.jdbc = jdbc;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public CategorySummary summary(Authentication authentication) {
        if (!permissions.has(authentication, "order:view")) {
            throw new AccessDeniedException("缺少订单查看权限");
        }
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE WHEN r.category = 'DELIVERY' THEN 1 ELSE 0 END), 0) AS delivery_count,
                       COALESCE(SUM(CASE WHEN r.category = 'CANCELLATION' THEN 1 ELSE 0 END), 0) AS cancellation_count,
                       COALESCE(SUM(CASE WHEN r.category = 'REFUND_REVIEW' THEN 1 ELSE 0 END), 0) AS refund_review_count,
                       COALESCE(SUM(CASE WHEN r.category = 'OTHER' THEN 1 ELSE 0 END), 0) AS other_count
                  FROM customer_order_after_sales_records r
                  JOIN customer_orders o ON o.order_no = r.order_no
                  LEFT JOIN xw_business_ownership own
                         ON own.business_no = COALESCE(o.business_no, o.order_no)
                  LEFT JOIN xw_departments d ON d.id = own.department_id
                 WHERE %s
                """.formatted(scopeSql(access)),
                (rs, rowNum) -> new CategorySummary(
                        rs.getLong("delivery_count"),
                        rs.getLong("cancellation_count"),
                        rs.getLong("refund_review_count"),
                        rs.getLong("other_count")),
                scopeArgs(access).toArray());
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

    public record CategorySummary(long deliveryCount, long cancellationCount,
                                  long refundReviewCount, long otherCount) {}
}
