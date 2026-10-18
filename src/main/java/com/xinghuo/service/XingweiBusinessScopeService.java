package com.xinghuo.service;

import com.xinghuo.account.service.AfterSalesLedger;
import com.xinghuo.account.service.AfterSalesNoteParser;
import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 星维 V2D 询价/订单统一业务归属与数据范围。
 *
 * <p>不修改 quotes/customer_orders 业务表。归属以统一 business_no 记录在
 * xw_business_ownership，询价转订单后会自然沿用同一归属记录。
 */
@Service
public class XingweiBusinessScopeService {
    private static final int PAGE_LIMIT = 100;
    private static final int AFTER_SALES_PAGE_SIZE = 50;
    private static final int MAX_AFTER_SALES_PAGE = 1000;

    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard permissions;
    private final AuditService audits;

    public XingweiBusinessScopeService(
            JdbcTemplate jdbc,
            XingweiPermissionGuard permissions,
            AuditService audits) {
        this.jdbc = jdbc;
        this.permissions = permissions;
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> quoteSnapshot(Authentication authentication) {
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        ScopeClause scope = scopeClause(access);
        String sql = """
                SELECT q.business_no, q.customer_account_id, q.game, q.demand, q.current_progress, q.customer_supplement,
                       q.platform, q.expected_time, q.contact_method, q.customer_contact,
                       q.customer_ip_hash, q.status, q.created_at,
                       (q.screenshot_asset_id IS NOT NULL) AS has_screenshot,
                       o.owner_admin_user_id,
                       owner.username AS owner_username,
                       owner_profile.display_name AS owner_display_name,
                       d.code AS department_code,
                       d.name AS department_name
                FROM quotes q
                LEFT JOIN xw_business_ownership o ON o.business_no = q.business_no
                LEFT JOIN admin_users owner ON owner.id = o.owner_admin_user_id
                LEFT JOIN xw_admin_profiles owner_profile ON owner_profile.admin_user_id = owner.id
                LEFT JOIN xw_departments d ON d.id = o.department_id
                WHERE %s
                ORDER BY q.created_at DESC
                LIMIT %d
                """.formatted(scope.sql(), PAGE_LIMIT);

        BusinessPermissions operations = businessPermissions(authentication);
        List<Map<String, Object>> items = jdbc.query(
                sql,
                (rs, rowNum) -> quoteRow(rs, access, operations),
                scope.args().toArray());
        return snapshot(access, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> orderSnapshot(Authentication authentication) {
        return orderSnapshot(authentication, OrderFilter.ALL);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exceptionOrderSnapshot(Authentication authentication) {
        return orderSnapshot(authentication, OrderFilter.EXCEPTIONS);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> afterSalesOrderSnapshot(Authentication authentication) {
        return afterSalesOrderSnapshot(authentication, null, 0);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> afterSalesOrderSnapshot(Authentication authentication, String query, int page) {
        return afterSalesOrderSnapshot(authentication, query, page, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> afterSalesOrderSnapshot(
            Authentication authentication, String query, int page, String category) {
        if (page < 0 || page > MAX_AFTER_SALES_PAGE) {
            throw new IllegalArgumentException("页码超出范围");
        }
        String keyword = query == null ? "" : query.trim();
        if (keyword.length() > 80) {
            throw new IllegalArgumentException("搜索内容不能超过 80 字");
        }
        String categoryLabel = switch (category == null ? "" : category) {
            case "" -> null;
            case "DELIVERY" -> "交付或进度";
            case "CANCELLATION" -> "取消服务";
            case "REFUND_REVIEW" -> "退款核对";
            case "OTHER" -> "其他问题";
            default -> throw new IllegalArgumentException("问题类型无效");
        };

        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        ScopeClause scope = scopeClause(access);
        String restriction = scope.sql().replace("o.", "own.")
                + " AND (LOCATE('[售后问题 ·', COALESCE(o2.customer_note, '')) > 0"
                + " OR EXISTS (SELECT 1 FROM customer_order_after_sales_records ar WHERE ar.order_no = o2.order_no))";
        List<Object> args = new ArrayList<>(scope.args());
        if (categoryLabel != null) {
            restriction += " AND (LOCATE(?, COALESCE(o2.customer_note, '')) > 0"
                    + " OR EXISTS (SELECT 1 FROM customer_order_after_sales_records ar"
                    + " WHERE ar.order_no = o2.order_no AND ar.category = ?))";
            args.add("[售后问题 · " + categoryLabel + " · ");
            args.add(category);
        }
        if (!keyword.isEmpty()) {
            restriction += " AND (LOCATE(?, COALESCE(o2.business_no, o2.order_no)) > 0"
                    + " OR LOCATE(?, o2.order_no) > 0"
                    + " OR LOCATE(?, COALESCE(o2.customer_note, '')) > 0"
                    + " OR EXISTS (SELECT 1 FROM customer_order_after_sales_records ar"
                    + " WHERE ar.order_no = o2.order_no AND LOCATE(?, ar.content) > 0))";
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }

        String from = """
                FROM customer_orders o2
                LEFT JOIN xw_business_ownership own
                       ON own.business_no = COALESCE(o2.business_no, o2.order_no)
                LEFT JOIN xw_departments d ON d.id = own.department_id
                WHERE %s
                """.formatted(restriction);
        Long count = jdbc.queryForObject("SELECT COUNT(*) " + from, Long.class, args.toArray());
        long totalCount = count == null ? 0L : count;
        String sql = """
                SELECT COALESCE(o2.business_no, o2.order_no) AS business_no,
                       o2.order_no, o2.status, o2.progress_percent, o2.progress_text,
                       (o2.progress_image_asset_id IS NOT NULL) AS has_progress_image,
                       o2.customer_note, o2.internal_note, o2.updated_at, o2.version,
                       own.owner_admin_user_id,
                       owner.username AS owner_username,
                       owner_profile.display_name AS owner_display_name,
                       d.code AS department_code,
                       d.name AS department_name
                FROM customer_orders o2
                LEFT JOIN xw_business_ownership own
                       ON own.business_no = COALESCE(o2.business_no, o2.order_no)
                LEFT JOIN admin_users owner ON owner.id = own.owner_admin_user_id
                LEFT JOIN xw_admin_profiles owner_profile ON owner_profile.admin_user_id = owner.id
                LEFT JOIN xw_departments d ON d.id = own.department_id
                WHERE %s
                ORDER BY o2.updated_at DESC, o2.id DESC
                LIMIT ? OFFSET ?
                """.formatted(restriction);
        args.add(AFTER_SALES_PAGE_SIZE);
        args.add(page * AFTER_SALES_PAGE_SIZE);
        BusinessPermissions operations = businessPermissions(authentication);
        List<Map<String, Object>> items = jdbc.query(
                sql, (rs, rowNum) -> orderRow(rs, access, operations), args.toArray());
        List<String> orderNos = items.stream().map(row -> (String) row.get("orderNo")).toList();
        Map<String, List<AfterSalesNoteParser.AfterSalesRecord>> stored =
                AfterSalesLedger.recordsForOrders(jdbc, orderNos);
        for (Map<String, Object> row : items) {
            String orderNo = (String) row.get("orderNo");
            row.put("afterSalesRecords", AfterSalesLedger.combine(
                    (String) row.get("customerNote"), stored.getOrDefault(orderNo, List.of())));
        }
        Map<String, Object> result = snapshot(access, items);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("pageSize", AFTER_SALES_PAGE_SIZE);
        result.put("category", category == null ? "" : category);
        result.put("hasNext", page < MAX_AFTER_SALES_PAGE
                && ((long) page + 1) * AFTER_SALES_PAGE_SIZE < totalCount);
        return result;
    }

    private enum OrderFilter { ALL, EXCEPTIONS }

    private Map<String, Object> orderSnapshot(Authentication authentication, OrderFilter filter) {
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        ScopeClause scope = scopeClause(access);
        String filterSql = switch (filter) {
            case ALL -> "";
            case EXCEPTIONS -> " AND o2.status = 'EXCEPTION'";
        };
        String restriction = scope.sql().replace("o.", "own.") + filterSql;
        String sql = """
                SELECT COALESCE(o2.business_no, o2.order_no) AS business_no,
                       o2.order_no, o2.status, o2.progress_percent, o2.progress_text,
                       (o2.progress_image_asset_id IS NOT NULL) AS has_progress_image,
                       o2.customer_note, o2.internal_note, o2.updated_at, o2.version,
                       o2.source_quote_id, o2.customer_account_id AS order_customer_account_id,
                       q.customer_account_id AS source_quote_account_id, q.status AS source_quote_status,
                       own.owner_admin_user_id,
                       owner.username AS owner_username,
                       owner_profile.display_name AS owner_display_name,
                       d.code AS department_code,
                       d.name AS department_name
                FROM customer_orders o2
                LEFT JOIN quotes q ON q.id = o2.source_quote_id
                LEFT JOIN xw_business_ownership own
                       ON own.business_no = COALESCE(o2.business_no, o2.order_no)
                LEFT JOIN admin_users owner ON owner.id = own.owner_admin_user_id
                LEFT JOIN xw_admin_profiles owner_profile ON owner_profile.admin_user_id = owner.id
                LEFT JOIN xw_departments d ON d.id = own.department_id
                WHERE %s
                ORDER BY o2.updated_at DESC
                LIMIT %d
                """.formatted(restriction, PAGE_LIMIT);

        BusinessPermissions operations = businessPermissions(authentication);
        List<Map<String, Object>> items = jdbc.query(
                sql,
                (rs, rowNum) -> {
                    Map<String, Object> row = orderRow(rs, access, operations);
                    populateOrderGuestOffer(row, rs, access, operations);
                    return row;
                },
                scope.args().toArray());
        return snapshot(access, items);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ownershipCandidates(Authentication authentication) {
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        if ("DENIED".equals(access.mode())) {
            return List.of();
        }

        String restriction;
        List<Object> args = new ArrayList<>();
        if (access.allowAll() || "ALL".equals(access.dataScope())) {
            restriction = "1=1";
        } else if ("DEPARTMENT".equals(access.dataScope()) && access.departmentCode() != null) {
            restriction = "d.code = ?";
            args.add(access.departmentCode());
        } else if ("SELF".equals(access.dataScope()) && access.adminUserId() != null) {
            restriction = "u.id = ?";
            args.add(access.adminUserId());
        } else {
            return List.of();
        }

        String sql = """
                SELECT u.id, u.username, p.display_name, p.employee_no,
                       d.code AS department_code, d.name AS department_name,
                       pos.code AS position_code, pos.name AS position_name
                FROM admin_users u
                JOIN xw_admin_profiles p ON p.admin_user_id = u.id
                JOIN xw_departments d ON d.id = p.department_id AND d.enabled = TRUE
                LEFT JOIN xw_positions pos ON pos.id = p.position_id AND pos.enabled = TRUE
                WHERE u.enabled = TRUE
                  AND p.status = 'ACTIVE'
                  AND %s
                ORDER BY d.sort_order, COALESCE(p.display_name, u.username), u.id
                """.formatted(restriction);
        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("adminUserId", rs.getLong("id"));
            row.put("username", rs.getString("username"));
            row.put("displayName", rs.getString("display_name"));
            row.put("employeeNo", rs.getString("employee_no"));
            row.put("departmentCode", rs.getString("department_code"));
            row.put("departmentName", rs.getString("department_name"));
            row.put("positionCode", rs.getString("position_code"));
            row.put("positionName", rs.getString("position_name"));
            return row;
        }, args.toArray());
    }

    @Transactional
    public Map<String, Object> assignQuote(
            String businessNo,
            Long ownerAdminUserId,
            String departmentCode,
            Authentication authentication,
            HttpServletRequest request) {
        String normalized = requireBusiness("quotes", businessNo);
        return assign(
                normalized,
                ownerAdminUserId,
                departmentCode,
                authentication,
                "quote:manage",
                "XW_QUOTE_OWNERSHIP_UPDATE",
                request);
    }

    @Transactional
    public Map<String, Object> assignOrder(
            String businessNo,
            Long ownerAdminUserId,
            String departmentCode,
            Authentication authentication,
            HttpServletRequest request) {
        String normalized = requireBusiness("customer_orders", businessNo);
        return assign(
                normalized,
                ownerAdminUserId,
                departmentCode,
                authentication,
                "order:assign",
                "XW_ORDER_OWNERSHIP_UPDATE",
                request);
    }

    @Transactional(readOnly = true)
    public boolean canAccessBusiness(Authentication authentication, String businessNo) {
        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        return canAccess(access, ownership(normalizeBusinessNo(businessNo)));
    }

    private Map<String, Object> assign(
            String businessNo,
            Long ownerAdminUserId,
            String requestedDepartmentCode,
            Authentication authentication,
            String requiredPermission,
            String auditAction,
            HttpServletRequest request) {
        if (!permissions.has(authentication, requiredPermission)) {
            throw new AccessDeniedException("缺少业务归属权限");
        }

        XingweiPermissionGuard.AccessSnapshot access = permissions.resolve(authentication);
        Ownership before = ownership(businessNo);
        if (!(access.allowAll() || "ALL".equals(access.dataScope())) && !canAccess(access, before)) {
            throw new AccessDeniedException("当前业务未归属到你的数据范围");
        }

        Destination destination = destination(ownerAdminUserId, requestedDepartmentCode);
        if (!canAssignTo(access, destination)) {
            throw new AccessDeniedException("目标负责人或中心超出当前数据范围");
        }

        int updated = jdbc.update(
                """
                UPDATE xw_business_ownership
                SET owner_admin_user_id = ?, department_id = ?, updated_by = ?, updated_at = NOW(6)
                WHERE business_no = ?
                """,
                destination.ownerAdminUserId(),
                destination.departmentId(),
                operator(authentication),
                businessNo);
        if (updated == 0) {
            try {
                jdbc.update(
                        """
                        INSERT INTO xw_business_ownership
                            (business_no, owner_admin_user_id, department_id,
                             created_by, updated_by, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, NOW(6), NOW(6))
                        """,
                        businessNo,
                        destination.ownerAdminUserId(),
                        destination.departmentId(),
                        operator(authentication),
                        operator(authentication));
            } catch (DuplicateKeyException race) {
                jdbc.update(
                        """
                        UPDATE xw_business_ownership
                        SET owner_admin_user_id = ?, department_id = ?, updated_by = ?, updated_at = NOW(6)
                        WHERE business_no = ?
                        """,
                        destination.ownerAdminUserId(),
                        destination.departmentId(),
                        operator(authentication),
                        businessNo);
            }
        }

        Ownership after = ownership(businessNo);
        audits.log(
                auditAction,
                "XW_BUSINESS_OWNERSHIP",
                businessNo,
                operator(authentication),
                before == null ? null : ownershipMap(before),
                ownershipMap(after),
                "星维 V2D 业务归属调整",
                request);
        return ownershipMap(after);
    }

    private Map<String, Object> snapshot(
            XingweiPermissionGuard.AccessSnapshot access,
            List<Map<String, Object>> items) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enforcement", "XINGWEI_V2D_BUSINESS_SCOPE");
        result.put("dataScope", access.dataScope());
        result.put("departmentCode", access.departmentCode());
        result.put("adminUserId", access.adminUserId());
        result.put("items", items);
        result.put("visibleCount", items.size());
        result.put("unassignedVisible", access.allowAll() || "ALL".equals(access.dataScope()));
        result.put("unassignedCount", items.stream()
                .filter(row -> {
                    Object ownership = row.get("ownership");
                    return ownership instanceof Map<?, ?> map && !Boolean.TRUE.equals(map.get("assigned"));
                })
                .count());
        return result;
    }

    private record BusinessPermissions(boolean quoteManage, boolean orderManage, boolean orderAssign) {}
    private BusinessPermissions businessPermissions(Authentication authentication) {
        return new BusinessPermissions(permissions.has(authentication, "quote:manage"),
                permissions.has(authentication, "order:manage"), permissions.has(authentication, "order:assign"));
    }

    private Map<String, Object> quoteRow(ResultSet rs, XingweiPermissionGuard.AccessSnapshot access, BusinessPermissions operations) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        String businessNo = rs.getString("business_no");
        String progress = rs.getString("current_progress");
        String supplement = rs.getString("customer_supplement");
        if (supplement != null && !supplement.isBlank()) {
            progress = (progress == null || progress.isBlank())
                    ? "客户补充：" + supplement
                    : progress + "\n客户补充：" + supplement;
        }
        Ownership ownership = ownershipFromJoinedRow(rs, businessNo);
        row.put("businessNo", businessNo);
        row.put("game", rs.getString("game"));
        row.put("demand", rs.getString("demand"));
        row.put("currentProgress", progress);
        row.put("platform", rs.getString("platform"));
        row.put("expectedTime", rs.getString("expected_time"));
        row.put("contactMethod", rs.getString("contact_method"));
        row.put("customerContact", rs.getString("customer_contact"));
        row.put("customerIpHash", rs.getString("customer_ip_hash"));
        row.put("status", rs.getString("status"));
        boolean anonymous = rs.getObject("customer_account_id") == null;
        row.put("customerType", anonymous ? "GUEST" : "ACCOUNT");
        boolean allowedToManage = operations.quoteManage()
                && canAccessForMutation(access, ownership);
        boolean closed = List.of("CLOSED", "CANCELLED").contains(rs.getString("status"));
        boolean converted = "CONVERTED".equals(rs.getString("status"));
        boolean hasValidOrder = !converted || hasUnpaidGuestSourceOrder(businessNo);
        boolean hasActivePayment = converted && hasValidOrder && hasActiveWechatPayment(businessNo);
        boolean canOffer = anonymous && allowedToManage && !closed
                && hasValidOrder && !hasActivePayment;
        row.put("canOffer", canOffer);
        row.put("guestOfferReason", !anonymous ? "这是注册账号询价，请使用账号报价流程"
                : closed ? "询价已关闭或取消，不能继续报价"
                : !allowedToManage ? "当前账号缺少此业务的报价权限"
                : !hasValidOrder ? "已转单记录缺少可核对的游客订单"
                : hasActivePayment ? "存在待支付或已付款单，请先到支付工作台核对"
                : "");
        row.put("createdAt", timestamp(rs, "created_at"));
        row.put("hasScreenshot", rs.getBoolean("has_screenshot"));
        row.put("ownership", ownershipMap(ownership));
        row.put("canManage", operations.quoteManage()
                && !"CONVERTED".equals(rs.getString("status"))
                && canAccessForMutation(access, ownership));
        row.put("canConvert", operations.quoteManage() && operations.orderManage()
                && rs.getString("status") != null && !List.of("CONVERTED", "CLOSED", "CANCELLED").contains(rs.getString("status"))
                && canAccessForMutation(access, ownership));
        row.put("canAssign", operations.quoteManage()
                && canAccessForMutation(access, ownership));
        return row;
    }

    /** Order-first guest checkout: provide the same offer action on its order workbench row. */
    private void populateOrderGuestOffer(Map<String, Object> row, ResultSet rs,
            XingweiPermissionGuard.AccessSnapshot access,
            BusinessPermissions operations) throws SQLException {
        String sourceQuoteId = rs.getString("source_quote_id");
        if (sourceQuoteId == null) {
            return; // orders created without a quote have no guest quote to price
        }
        String no = rs.getString("business_no");
        boolean guest = rs.getObject("order_customer_account_id") == null
                && rs.getObject("source_quote_account_id") == null;
        boolean active = !List.of("CLOSED", "CANCELLED").contains(rs.getString("source_quote_status"));
        boolean permission = operations.quoteManage()
                && canAccessForMutation(access, ownershipFromJoinedRow(rs, no));
        boolean pending = guest && active && permission && hasActiveWechatPayment(no);
        row.put("guestQuoteLinked", true);
        row.put("canOffer", guest && active && permission && !pending);
        row.put("guestOfferReason", !guest ? "注册账号订单不能使用游客报价"
                : !active ? "来源询价已关闭或取消"
                : !permission ? "当前账号没有此订单的询价报价权限"
                : pending ? "存在待支付或已付款单，请先到支付工作台核对"
                : "");
    }

    private boolean hasUnpaidGuestSourceOrder(String no) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM customer_orders o
                  JOIN quotes q ON q.id=o.source_quote_id
                 WHERE o.business_no=? AND q.business_no=?
                   AND o.customer_account_id IS NULL AND q.customer_account_id IS NULL
                """, Integer.class, no, no);
        return count != null && count == 1;
    }

    private boolean hasActiveWechatPayment(String no) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payment_order
                 WHERE business_no=? AND provider='WECHAT'
                   AND status IN ('CREATED','APPROVED','CAPTURED')
                """, Integer.class, no);
        return count != null && count > 0;
    }

    private Map<String, Object> orderRow(ResultSet rs, XingweiPermissionGuard.AccessSnapshot access, BusinessPermissions operations) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        String businessNo = rs.getString("business_no");
        Ownership ownership = ownershipFromJoinedRow(rs, businessNo);
        row.put("businessNo", businessNo);
        row.put("orderNo", rs.getString("order_no"));
        row.put("status", rs.getString("status"));
        row.put("progressPercent", nullableInteger(rs, "progress_percent"));
        row.put("progressText", rs.getString("progress_text"));
        row.put("hasProgressImage", rs.getBoolean("has_progress_image"));
        row.put("customerNote", rs.getString("customer_note"));
        row.put("internalNote", rs.getString("internal_note"));
        row.put("updatedAt", timestamp(rs, "updated_at"));
        row.put("version", rs.getLong("version"));
        row.put("ownership", ownershipMap(ownership));
        row.put("canManage", operations.orderManage()
                && canAccessForMutation(access, ownership));
        row.put("canAssign", operations.orderAssign()
                && canAccessForMutation(access, ownership));
        return row;
    }

    private Ownership ownershipFromJoinedRow(ResultSet rs, String businessNo) throws SQLException {
        Object ownerIdValue = rs.getObject("owner_admin_user_id");
        Long ownerId = ownerIdValue instanceof Number number ? number.longValue() : null;
        String departmentCode = rs.getString("department_code");
        if (departmentCode == null) {
            return null;
        }
        return new Ownership(
                businessNo,
                ownerId,
                rs.getString("owner_username"),
                rs.getString("owner_display_name"),
                departmentCode,
                rs.getString("department_name"));
    }

    private Ownership ownership(String businessNo) {
        if (businessNo == null) {
            return null;
        }
        List<Ownership> rows = jdbc.query(
                """
                SELECT o.business_no, o.owner_admin_user_id,
                       owner.username AS owner_username,
                       p.display_name AS owner_display_name,
                       d.code AS department_code, d.name AS department_name
                FROM xw_business_ownership o
                LEFT JOIN admin_users owner ON owner.id = o.owner_admin_user_id
                LEFT JOIN xw_admin_profiles p ON p.admin_user_id = owner.id
                JOIN xw_departments d ON d.id = o.department_id
                WHERE o.business_no = ?
                """,
                (rs, rowNum) -> {
                    Object ownerIdValue = rs.getObject("owner_admin_user_id");
                    Long ownerId = ownerIdValue instanceof Number number ? number.longValue() : null;
                    return new Ownership(
                            rs.getString("business_no"),
                            ownerId,
                            rs.getString("owner_username"),
                            rs.getString("owner_display_name"),
                            rs.getString("department_code"),
                            rs.getString("department_name"));
                },
                businessNo);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private Destination destination(Long ownerAdminUserId, String requestedDepartmentCode) {
        String normalizedDepartment = blankToNull(requestedDepartmentCode);
        if (ownerAdminUserId != null) {
            List<Destination> candidates = jdbc.query(
                    """
                    SELECT u.id AS owner_admin_user_id, d.id AS department_id, d.code AS department_code
                    FROM admin_users u
                    JOIN xw_admin_profiles p ON p.admin_user_id = u.id
                    JOIN xw_departments d ON d.id = p.department_id AND d.enabled = TRUE
                    WHERE u.id = ? AND u.enabled = TRUE AND p.status = 'ACTIVE'
                    """,
                    (rs, rowNum) -> new Destination(
                            rs.getLong("owner_admin_user_id"),
                            rs.getLong("department_id"),
                            rs.getString("department_code")),
                    ownerAdminUserId);
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("负责人不存在、未启用或尚未配置在岗中心");
            }
            Destination destination = candidates.getFirst();
            if (normalizedDepartment != null
                    && !normalizedDepartment.equalsIgnoreCase(destination.departmentCode())) {
                throw new IllegalArgumentException("负责人所属中心与指定中心不一致");
            }
            return destination;
        }

        if (normalizedDepartment == null) {
            throw new IllegalArgumentException("请选择负责人或所属中心");
        }
        try {
            Map<String, Object> department = jdbc.queryForMap(
                    "SELECT id, code FROM xw_departments WHERE code = ? AND enabled = TRUE",
                    normalizedDepartment);
            return new Destination(
                    null,
                    ((Number) department.get("id")).longValue(),
                    String.valueOf(department.get("code")));
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("所属中心不存在或已停用");
        }
    }

    private boolean canAccess(XingweiPermissionGuard.AccessSnapshot access, Ownership ownership) {
        if ("DENIED".equals(access.mode())) {
            return false;
        }
        if (access.allowAll() || "ALL".equals(access.dataScope())) {
            return true;
        }
        if (ownership == null) {
            return false;
        }
        if ("DEPARTMENT".equals(access.dataScope())) {
            return access.departmentCode() != null
                    && access.departmentCode().equals(ownership.departmentCode());
        }
        return "SELF".equals(access.dataScope())
                && access.adminUserId() != null
                && access.adminUserId().equals(ownership.ownerAdminUserId());
    }

    private boolean canAccessForMutation(
            XingweiPermissionGuard.AccessSnapshot access,
            Ownership ownership) {
        if (access.allowAll() || "ALL".equals(access.dataScope())) {
            return true;
        }
        return canAccess(access, ownership);
    }

    private boolean canAssignTo(
            XingweiPermissionGuard.AccessSnapshot access,
            Destination destination) {
        if (access.allowAll() || "ALL".equals(access.dataScope())) {
            return true;
        }
        if ("DEPARTMENT".equals(access.dataScope())) {
            return access.departmentCode() != null
                    && access.departmentCode().equals(destination.departmentCode());
        }
        return "SELF".equals(access.dataScope())
                && access.adminUserId() != null
                && access.adminUserId().equals(destination.ownerAdminUserId());
    }

    ScopeClause queryScope(Authentication actor) { return scopeClause(permissions.resolve(actor)); }

    private ScopeClause scopeClause(XingweiPermissionGuard.AccessSnapshot access) {
        if (access.allowAll() || "ALL".equals(access.dataScope())) {
            return new ScopeClause("1=1", List.of());
        }
        if ("DEPARTMENT".equals(access.dataScope()) && access.departmentCode() != null) {
            return new ScopeClause("d.code = ?", List.of(access.departmentCode()));
        }
        if ("SELF".equals(access.dataScope()) && access.adminUserId() != null) {
            return new ScopeClause("o.owner_admin_user_id = ?", List.of(access.adminUserId()));
        }
        return new ScopeClause("1=0", List.of());
    }

    private String requireBusiness(String table, String businessNo) {
        String normalized = normalizeBusinessNo(businessNo);
        if (normalized == null) {
            throw new IllegalArgumentException("业务编号不能为空");
        }
        String sql = "quotes".equals(table)
                ? "SELECT COUNT(*) FROM quotes WHERE business_no = ?"
                : "SELECT COUNT(*) FROM customer_orders WHERE COALESCE(business_no, order_no) = ?";
        Long count = jdbc.queryForObject(sql, Long.class, normalized);
        if (count == null || count == 0L) {
            throw new IllegalArgumentException("业务记录不存在");
        }
        return normalized;
    }

    private static Map<String, Object> ownershipMap(Ownership ownership) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assigned", ownership != null);
        result.put("businessNo", ownership == null ? null : ownership.businessNo());
        result.put("ownerAdminUserId", ownership == null ? null : ownership.ownerAdminUserId());
        result.put("ownerUsername", ownership == null ? null : ownership.ownerUsername());
        result.put("ownerDisplayName", ownership == null ? null : ownership.ownerDisplayName());
        result.put("departmentCode", ownership == null ? null : ownership.departmentCode());
        result.put("departmentName", ownership == null ? null : ownership.departmentName());
        return result;
    }

    private static Object timestamp(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static String normalizeBusinessNo(String value) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned.toUpperCase(Locale.ROOT);
        if (cleaned.length() > 20 || !cleaned.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("业务编号格式不合法");
        }
        return cleaned;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String operator(Authentication authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }

    record ScopeClause(String sql, List<Object> args) {}

    private record Destination(Long ownerAdminUserId, long departmentId, String departmentCode) {}

    private record Ownership(
            String businessNo,
            Long ownerAdminUserId,
            String ownerUsername,
            String ownerDisplayName,
            String departmentCode,
            String departmentName) {}
}
