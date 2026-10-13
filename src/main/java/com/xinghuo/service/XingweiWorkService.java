package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 星维 V2F：派单后的员工工作台。
 *
 * <p>工作台不复制订单状态机，也不新增派单表。任务来源仍是 customer_orders，
 * 归属继续复用 xw_business_ownership；履约更新最终委托既有 OrderService，
 * 这里只负责“本人 / 中心队列 / 全局未归属”的数据范围与动作边界。
 */
@Service
public class XingweiWorkService {
    private static final int PAGE_LIMIT = 100;

    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard permissions;
    private final XingweiBusinessScopeService businessScope;
    private final OrderService orders;

    public XingweiWorkService(
            JdbcTemplate jdbc,
            XingweiPermissionGuard permissions,
            XingweiBusinessScopeService businessScope,
            OrderService orders) {
        this.jdbc = jdbc;
        this.permissions = permissions;
        this.businessScope = businessScope;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot(Authentication authentication) {
        if (!permissions.has(authentication, "order:view")) {
            throw new AccessDeniedException("缺少订单查看权限");
        }
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        WorkClause clause = workClause(access);

        String sql = """
                SELECT COALESCE(o.business_no, o.order_no) AS business_no,
                       o.order_no,
                       q.game,
                       q.demand,
                       q.expected_time,
                       o.status,
                       o.progress_percent,
                       o.progress_text,
                       o.customer_note,
                       o.internal_note,
                       o.updated_at,
                       o.version,
                       own.owner_admin_user_id,
                       owner.username AS owner_username,
                       owner_profile.display_name AS owner_display_name,
                       d.code AS department_code,
                       d.name AS department_name
                FROM customer_orders o
                LEFT JOIN quotes q ON q.id = o.source_quote_id
                LEFT JOIN xw_business_ownership own
                       ON own.business_no = COALESCE(o.business_no, o.order_no)
                LEFT JOIN admin_users owner ON owner.id = own.owner_admin_user_id
                LEFT JOIN xw_admin_profiles owner_profile ON owner_profile.admin_user_id = owner.id
                LEFT JOIN xw_departments d ON d.id = own.department_id
                WHERE %s
                ORDER BY o.updated_at DESC
                LIMIT %d
                """.formatted(clause.sql(), PAGE_LIMIT);

        List<Map<String, Object>> items = jdbc.query(
                sql,
                (rs, rowNum) -> workRow(rs, access, authentication),
                clause.args().toArray());

        long mine = items.stream().filter(row -> "MINE".equals(row.get("bucket"))).count();
        long queue = items.stream().filter(row -> "DEPARTMENT_QUEUE".equals(row.get("bucket"))).count();
        long unassigned = items.stream().filter(row -> "UNASSIGNED".equals(row.get("bucket"))).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enforcement", "XINGWEI_V2F_WORKBENCH");
        result.put("dataScope", access.dataScope());
        result.put("departmentCode", access.departmentCode());
        result.put("adminUserId", access.adminUserId());
        result.put("items", items);
        result.put("visibleCount", items.size());
        result.put("mineCount", mine);
        result.put("queueCount", queue);
        result.put("unassignedCount", unassigned);
        return result;
    }

    @Transactional
    public Map<String, Object> claim(
            String businessNo,
            Authentication authentication,
            HttpServletRequest request) {
        if (!permissions.has(authentication, "order:assign")) {
            throw new AccessDeniedException("缺少订单派单权限");
        }
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        if (access.adminUserId() == null || !selfIsActiveEmployee(access.adminUserId())) {
            throw new AccessDeniedException("当前账号没有可领取任务的有效员工档案");
        }

        String normalized = normalizeBusinessNo(businessNo);
        Ownership current = ownership(normalized);
        if (current != null && current.ownerAdminUserId() != null) {
            throw new AccessDeniedException("任务已由员工领取，请使用业务工作台转派");
        }
        if (!businessScope.canAccessBusiness(authentication, normalized)) {
            throw new AccessDeniedException("当前任务不在你的数据范围");
        }

        return businessScope.assignOrder(
                normalized,
                access.adminUserId(),
                null,
                authentication,
                request);
    }

    @Transactional
    public ApiDtos.AdminOrderResponse updateOrder(
            String businessNo,
            ApiDtos.UpdateOrderRequest update,
            Authentication authentication,
            HttpServletRequest request) {
        if (!permissions.has(authentication, "order:manage")) {
            throw new AccessDeniedException("缺少订单履约修改权限");
        }
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        if (access.adminUserId() == null) {
            throw new AccessDeniedException("当前账号没有员工身份");
        }

        String normalized = normalizeBusinessNo(businessNo);
        Ownership current = ownership(normalized);
        if (current == null || current.ownerAdminUserId() == null
                || !current.ownerAdminUserId().equals(access.adminUserId())) {
            throw new AccessDeniedException("员工工作台只允许修改本人已领取任务");
        }
        if (!businessScope.canAccessBusiness(authentication, normalized)) {
            throw new AccessDeniedException("当前任务不在你的数据范围");
        }

        String orderNo = jdbc.query(
                """
                SELECT order_no
                FROM customer_orders
                WHERE COALESCE(business_no, order_no) = ?
                """,
                (rs, rowNum) -> rs.getString("order_no"),
                normalized).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        return orders.update(orderNo, update, operator(authentication), request);
    }

    private Map<String, Object> workRow(
            ResultSet rs,
            XingweiPermissionGuard.AccessSnapshot access,
            Authentication authentication) throws SQLException {
        Object ownerValue = rs.getObject("owner_admin_user_id");
        Long ownerId = ownerValue instanceof Number number ? number.longValue() : null;
        String departmentCode = rs.getString("department_code");
        String bucket = bucket(ownerId, departmentCode, access.adminUserId());
        boolean mine = "MINE".equals(bucket);
        boolean canClaim = !mine
                && ownerId == null
                && permissions.has(authentication, "order:assign")
                && access.adminUserId() != null
                && selfIsActiveEmployee(access.adminUserId());

        Map<String, Object> ownership = new LinkedHashMap<>();
        ownership.put("assigned", departmentCode != null);
        ownership.put("ownerAdminUserId", ownerId);
        ownership.put("ownerUsername", rs.getString("owner_username"));
        ownership.put("ownerDisplayName", rs.getString("owner_display_name"));
        ownership.put("departmentCode", departmentCode);
        ownership.put("departmentName", rs.getString("department_name"));

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("businessNo", rs.getString("business_no"));
        row.put("orderNo", rs.getString("order_no"));
        row.put("game", rs.getString("game"));
        row.put("demand", rs.getString("demand"));
        row.put("expectedTime", rs.getString("expected_time"));
        row.put("status", rs.getString("status"));
        row.put("progressPercent", nullableInteger(rs, "progress_percent"));
        row.put("progressText", rs.getString("progress_text"));
        row.put("customerNote", rs.getString("customer_note"));
        row.put("internalNote", rs.getString("internal_note"));
        row.put("updatedAt", timestamp(rs, "updated_at"));
        row.put("version", rs.getLong("version"));
        row.put("bucket", bucket);
        row.put("ownership", ownership);
        row.put("canClaim", canClaim);
        row.put("canUpdate", mine && permissions.has(authentication, "order:manage"));
        return row;
    }

    private WorkClause workClause(XingweiPermissionGuard.AccessSnapshot access) {
        if ("DENIED".equals(access.mode()) || access.adminUserId() == null) {
            return new WorkClause("1=0", List.of());
        }
        if (access.allowAll() || "ALL".equals(access.dataScope())) {
            return new WorkClause(
                    "(own.owner_admin_user_id = ? OR own.owner_admin_user_id IS NULL)",
                    List.of(access.adminUserId()));
        }
        if ("DEPARTMENT".equals(access.dataScope()) && access.departmentCode() != null) {
            return new WorkClause(
                    "(own.owner_admin_user_id = ? OR (own.owner_admin_user_id IS NULL AND d.code = ?))",
                    List.of(access.adminUserId(), access.departmentCode()));
        }
        if ("SELF".equals(access.dataScope())) {
            return new WorkClause("own.owner_admin_user_id = ?", List.of(access.adminUserId()));
        }
        return new WorkClause("1=0", List.of());
    }

    private Ownership ownership(String businessNo) {
        return jdbc.query(
                """
                SELECT own.owner_admin_user_id, d.code AS department_code
                FROM xw_business_ownership own
                JOIN xw_departments d ON d.id = own.department_id
                WHERE own.business_no = ?
                """,
                (rs, rowNum) -> {
                    Object ownerValue = rs.getObject("owner_admin_user_id");
                    Long ownerId = ownerValue instanceof Number number ? number.longValue() : null;
                    return new Ownership(ownerId, rs.getString("department_code"));
                },
                businessNo).stream().findFirst().orElse(null);
    }

    private boolean selfIsActiveEmployee(Long adminUserId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM admin_users u
                JOIN xw_admin_profiles p ON p.admin_user_id = u.id
                JOIN xw_departments d ON d.id = p.department_id
                WHERE u.id = ?
                  AND u.enabled = TRUE
                  AND p.status = 'ACTIVE'
                  AND d.enabled = TRUE
                """,
                Integer.class,
                adminUserId);
        return count != null && count > 0;
    }

    private String bucket(Long ownerId, String departmentCode, Long currentAdminUserId) {
        if (ownerId != null && ownerId.equals(currentAdminUserId)) {
            return "MINE";
        }
        if (ownerId == null && departmentCode != null) {
            return "DEPARTMENT_QUEUE";
        }
        return "UNASSIGNED";
    }

    private String normalizeBusinessNo(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("业务编号不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 40 || !normalized.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("业务编号不合法");
        }
        return normalized;
    }

    private String operator(Authentication authentication) {
        return authentication == null || authentication.getName() == null
                ? "admin"
                : authentication.getName();
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value instanceof Number number ? number.intValue() : null;
    }

    private Object timestamp(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private record WorkClause(String sql, List<Object> args) {}

    private record Ownership(Long ownerAdminUserId, String departmentCode) {}
}
