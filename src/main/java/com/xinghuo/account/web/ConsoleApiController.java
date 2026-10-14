package com.xinghuo.account.web;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountAuditService;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.AfterSalesLedger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/console")
public class ConsoleApiController {
    private static final String AFTER_SALES_MARKER = "[售后问题 ·";

    private final JdbcTemplate jdbc;
    private final AccountAuditService audit;

    public ConsoleApiController(JdbcTemplate jdbc, AccountAuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @GetMapping("/summary")
    @RequireAccount
    @Transactional(readOnly = true)
    public SummaryResponse summary(HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        long quoteTotal = count("SELECT COUNT(*) FROM quotes WHERE customer_account_id = ?", account.getId());
        long orderTotal = count("SELECT COUNT(*) FROM customer_orders WHERE customer_account_id = ?", account.getId());
        long activeOrders = count("""
                SELECT COUNT(*) FROM customer_orders
                 WHERE customer_account_id = ? AND status IN ('WAITING_CONFIRMATION','IN_PROGRESS','EXCEPTION')
                """, account.getId());
        long securityEvents = count("SELECT COUNT(*) FROM user_audit_log WHERE user_id = ?", account.getId());
        return new SummaryResponse(quoteTotal, orderTotal, activeOrders, securityEvents,
                quotes(account, 0, 3).items(), orders(account, 0, 3).items(), loginLogs(account, 5));
    }

    @GetMapping("/quotes")
    @RequireAccount
    @Transactional(readOnly = true)
    public QuoteListResponse quotes(HttpServletRequest request) {
        return quotes(AccountAuthInterceptor.requireAccount(request), 0, 50);
    }

    @GetMapping("/quotes/{businessNo}")
    @RequireAccount
    @Transactional(readOnly = true)
    public QuoteItem quote(@PathVariable String businessNo, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return findQuote(account, businessNo);
    }

    @PostMapping("/quotes/{businessNo}/supplement")
    @RequireAccount
    @Transactional
    public OkResponse supplementQuote(@PathVariable String businessNo,
                                      @Valid @RequestBody SupplementRequest body,
                                      HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        findQuote(account, businessNo);
        int updated = jdbc.update("""
                UPDATE quotes
                   SET customer_supplement = CONCAT(COALESCE(customer_supplement, ''), ?), updated_at = CURRENT_TIMESTAMP(6)
                 WHERE customer_account_id = ? AND business_no = ?
                """, appendBlock(body.content()), account.getId(), businessNo);
        if (updated == 0) throw AccountException.notFound("QUOTE_NOT_FOUND", "需求不存在或不属于当前账号");
        audit.record(account.getId(), "CONSOLE_QUOTE_SUPPLEMENT", "businessNo=" + businessNo, request);
        return OkResponse.of("补充说明已提交");
    }

    @GetMapping("/orders")
    @RequireAccount
    @Transactional(readOnly = true)
    public OrderListResponse orders(HttpServletRequest request) {
        return orders(AccountAuthInterceptor.requireAccount(request), 0, 50);
    }

    @GetMapping("/orders/{businessNo}")
    @RequireAccount
    @Transactional(readOnly = true)
    public OrderDetailResponse order(@PathVariable String businessNo, HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        OrderItem item = findOrder(account, businessNo);
        List<TimelineItem> timeline = new ArrayList<>();
        timeline.add(new TimelineItem("订单创建", "系统已建立业务记录", item.createdAt()));
        if (item.progressText() != null && !item.progressText().isBlank()) {
            timeline.add(new TimelineItem("进度更新", item.progressText(), item.updatedAt()));
        }
        if ("COMPLETED".equals(item.status())) {
            timeline.add(new TimelineItem("交付完成", "订单已标记完成，如有问题可联系客服处理售后。", item.updatedAt()));
        }
        QuoteItem source = item.sourceQuoteBusinessNo() == null ? null : findQuoteOrNull(account, item.sourceQuoteBusinessNo());
        return new OrderDetailResponse(item, source, timeline,
                afterSalesRecords(item.orderNo(), item.customerNote()),
                afterSalesCases(item.orderNo()));
    }

    @PostMapping("/orders/{businessNo}/supplement")
    @RequireAccount
    @Transactional
    public OkResponse supplementOrder(@PathVariable String businessNo,
                                      @Valid @RequestBody SupplementRequest body,
                                      HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        OrderItem order = findOrder(account, businessNo);
        rejectReservedAfterSalesMarker(body.content());
        String note = appendBlock(body.content());
        int updated = jdbc.update("""
                UPDATE customer_orders
                   SET customer_note = CONCAT(COALESCE(customer_note, ''), ?), version = version + 1, updated_at = CURRENT_TIMESTAMP(6)
                 WHERE customer_account_id = ? AND (business_no = ? OR order_no = ?)
                   AND CHAR_LENGTH(COALESCE(customer_note, '')) + CHAR_LENGTH(?) <= 500
                """, note, account.getId(), order.businessNo(), order.orderNo(), note);
        if (updated == 0) {
            throw AccountException.conflict("ORDER_NOTE_FULL", "订单说明已满，请凭业务编号联系客服处理");
        }
        audit.record(account.getId(), "CONSOLE_ORDER_SUPPLEMENT", "businessNo=" + businessNo, request);
        return OkResponse.of("订单补充说明已提交");
    }

    @PostMapping("/orders/{businessNo}/after-sales")
    @RequireAccount
    @Transactional
    public OkResponse submitAfterSales(@PathVariable String businessNo,
                                       @Valid @RequestBody AfterSalesRequest body,
                                       HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        OrderItem order = findOrder(account, businessNo);
        String category = afterSalesCategory(body.category());
        String content = body.content().trim();
        if (content.isEmpty()) {
            throw AccountException.badRequest("AFTER_SALES_CONTENT_REQUIRED", "请填写问题说明");
        }
        rejectReservedAfterSalesMarker(content);
        Instant submittedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String note = "\n\n[售后问题 · " + category + " · " + submittedAt + "]\n" + content;
        int updated = jdbc.update("""
                UPDATE customer_orders
                   SET customer_note = CONCAT(COALESCE(customer_note, ''), ?),
                       version = version + 1, updated_at = CURRENT_TIMESTAMP(6)
                 WHERE customer_account_id = ? AND (business_no = ? OR order_no = ?)
                   AND CHAR_LENGTH(COALESCE(customer_note, '')) + CHAR_LENGTH(?) <= 500
                """, note, account.getId(), order.businessNo(), order.orderNo(), note);
        if (updated == 0) {
            updated = jdbc.update("""
                    UPDATE customer_orders
                       SET version = version + 1, updated_at = CURRENT_TIMESTAMP(6)
                     WHERE customer_account_id = ? AND (business_no = ? OR order_no = ?)
                    """, account.getId(), order.businessNo(), order.orderNo());
            if (updated == 0) {
                throw AccountException.notFound("ORDER_NOT_FOUND", "订单不存在或不属于当前账号");
            }
        }
        jdbc.update("""
                INSERT INTO customer_order_after_sales_records
                    (order_no, category, category_label, content, submitted_at_ms)
                VALUES (?, ?, ?, ?, ?)
                """, order.orderNo(), body.category(), category, content, submittedAt.toEpochMilli());
        audit.record(account.getId(), "CONSOLE_AFTER_SALES_SUBMIT",
                "businessNo=" + order.businessNo() + ",category=" + body.category(), request);
        return OkResponse.of("问题说明已提交，请等待客服核对");
    }

    @PostMapping("/orders/{businessNo}/after-sales/records/{recordId}/followups")
    @RequireAccount
    @Transactional
    public OkResponse followUpAfterSales(@PathVariable String businessNo, @PathVariable long recordId,
                                         @Valid @RequestBody AfterSalesFollowupRequest body,
                                         HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        OrderItem order = findOrder(account, businessNo);
        if (recordId <= 0) throw AccountException.badRequest("AFTER_SALES_RECORD_INVALID", "售后记录编号无效");
        String content = body.content().trim();
        if (content.isEmpty()) throw AccountException.badRequest("AFTER_SALES_CONTENT_REQUIRED", "请填写补充说明");
        if (content.length() > 240) throw AccountException.badRequest("AFTER_SALES_CONTENT_TOO_LONG", "补充说明不能超过 240 字");
        rejectReservedAfterSalesMarker(content);
        Long visible = jdbc.queryForObject("""
                SELECT COUNT(*) FROM customer_order_after_sales_records
                 WHERE id = ? AND order_no = ?
                """, Long.class, recordId, order.orderNo());
        if (visible == null || visible == 0) {
            throw AccountException.notFound("AFTER_SALES_RECORD_NOT_FOUND", "售后记录不存在或不属于当前订单");
        }
        jdbc.update("""
                INSERT INTO customer_order_after_sales_case_followups
                    (record_id, account_id, content, created_at_ms)
                VALUES (?, ?, ?, ?)
                """, recordId, account.getId(), content, Instant.now().toEpochMilli());
        audit.record(account.getId(), "CONSOLE_AFTER_SALES_FOLLOWUP",
                "businessNo=" + order.businessNo() + ",recordId=" + recordId, request);
        return OkResponse.of("补充说明已提交，客服将在原记录中核对");
    }

    @GetMapping("/security/logins")
    @RequireAccount
    @Transactional(readOnly = true)
    public List<LoginLogItem> securityLogins(HttpServletRequest request) {
        return loginLogs(AccountAuthInterceptor.requireAccount(request), 50);
    }

    private QuoteListResponse quotes(Account account, int page, int size) {
        List<QuoteItem> items = jdbc.query("""
                SELECT business_no, game, demand, current_progress, platform, expected_time,
                       status, customer_supplement, created_at, updated_at
                  FROM quotes
                 WHERE customer_account_id = ?
                 ORDER BY created_at DESC
                 LIMIT ? OFFSET ?
                """, (rs, i) -> new QuoteItem(
                rs.getString("business_no"), rs.getString("game"), rs.getString("demand"),
                rs.getString("current_progress"), rs.getString("platform"), rs.getString("expected_time"),
                rs.getString("status"), quoteStatusText(rs.getString("status")),
                rs.getString("customer_supplement"), toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))), account.getId(), size, (long) page * size);
        long total = count("SELECT COUNT(*) FROM quotes WHERE customer_account_id = ?", account.getId());
        return new QuoteListResponse(items, total, page, size);
    }

    private OrderListResponse orders(Account account, int page, int size) {
        List<OrderItem> items = jdbc.query("""
                SELECT o.order_no, o.business_no, o.source_quote_id, o.status, o.progress_percent,
                       o.progress_text, o.customer_note, o.created_at, o.updated_at,
                       q.business_no AS quote_business_no, q.game AS game_name, q.demand AS service_name
                  FROM customer_orders o
                  LEFT JOIN quotes q ON q.id = o.source_quote_id
                 WHERE o.customer_account_id = ?
                 ORDER BY o.created_at DESC
                 LIMIT ? OFFSET ?
                """, (rs, i) -> orderItem(rs), account.getId(), size, (long) page * size);
        long total = count("SELECT COUNT(*) FROM customer_orders WHERE customer_account_id = ?", account.getId());
        return new OrderListResponse(items, total, page, size);
    }

    private QuoteItem findQuote(Account account, String businessNo) {
        List<QuoteItem> rows = jdbc.query("""
                SELECT business_no, game, demand, current_progress, platform, expected_time,
                       status, customer_supplement, created_at, updated_at
                  FROM quotes
                 WHERE customer_account_id = ? AND business_no = ?
                 LIMIT 1
                """, (rs, i) -> new QuoteItem(
                rs.getString("business_no"), rs.getString("game"), rs.getString("demand"),
                rs.getString("current_progress"), rs.getString("platform"), rs.getString("expected_time"),
                rs.getString("status"), quoteStatusText(rs.getString("status")),
                rs.getString("customer_supplement"), toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))), account.getId(), businessNo);
        if (rows.isEmpty()) throw AccountException.notFound("QUOTE_NOT_FOUND", "需求不存在或不属于当前账号");
        return rows.get(0);
    }

    private QuoteItem findQuoteOrNull(Account account, String businessNo) {
        try { return findQuote(account, businessNo); } catch (AccountException ignored) { return null; }
    }

    private OrderItem findOrder(Account account, String businessNo) {
        List<OrderItem> rows = jdbc.query("""
                SELECT o.order_no, o.business_no, o.source_quote_id, o.status, o.progress_percent,
                       o.progress_text, o.customer_note, o.created_at, o.updated_at,
                       q.business_no AS quote_business_no, q.game AS game_name, q.demand AS service_name
                  FROM customer_orders o
                  LEFT JOIN quotes q ON q.id = o.source_quote_id
                 WHERE o.customer_account_id = ? AND (o.business_no = ? OR o.order_no = ?)
                 LIMIT 1
                """, (rs, i) -> orderItem(rs), account.getId(), businessNo, businessNo);
        if (rows.isEmpty()) throw AccountException.notFound("ORDER_NOT_FOUND", "订单不存在或不属于当前账号");
        return rows.get(0);
    }

    private OrderItem orderItem(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OrderItem(rs.getString("order_no"), rs.getString("business_no"),
                rs.getString("quote_business_no"), rs.getString("status"), orderStatusText(rs.getString("status")),
                (Integer) rs.getObject("progress_percent"), rs.getString("progress_text"),
                rs.getString("customer_note"), rs.getString("game_name"), rs.getString("service_name"),
                toInstant(rs.getTimestamp("created_at")), toInstant(rs.getTimestamp("updated_at")));
    }

    private List<LoginLogItem> loginLogs(Account account, int limit) {
        return jdbc.query("""
                SELECT action, detail, ip, user_agent, created_at
                  FROM user_audit_log
                 WHERE user_id = ? AND action LIKE 'LOGIN%'
                 ORDER BY created_at DESC
                 LIMIT ?
                """, (rs, i) -> new LoginLogItem(rs.getString("action"), rs.getString("detail"),
                rs.getString("ip"), rs.getString("user_agent"), toInstant(rs.getTimestamp("created_at"))),
                account.getId(), limit);
    }

    private long count(String sql, Object... args) {
        Long total = jdbc.queryForObject(sql, Long.class, args);
        return total == null ? 0 : total;
    }

    private static String appendBlock(String text) {
        return "\n\n[用户补充 " + Instant.now() + "]\n" + text.trim();
    }

    private static void rejectReservedAfterSalesMarker(String content) {
        if (content != null && content.contains(AFTER_SALES_MARKER)) {
            throw AccountException.badRequest("AFTER_SALES_MARKER_RESERVED", "说明中不能包含系统售后记录标记");
        }
    }

    private List<AfterSalesRecord> afterSalesRecords(String orderNo, String note) {
        return AfterSalesLedger.recordsForOrder(jdbc, orderNo, note).stream()
                .map(record -> new AfterSalesRecord(record.category(), record.submittedAt(), record.content()))
                .toList();
    }

    private List<AfterSalesCaseStatus> afterSalesCases(String orderNo) {
        Map<Long, List<AfterSalesCustomerFollowup>> followupsByRecord = jdbc.query("""
                SELECT f.record_id, f.content, f.created_at_ms
                  FROM customer_order_after_sales_case_followups f
                  JOIN customer_order_after_sales_records r ON r.id = f.record_id
                 WHERE r.order_no = ?
                 ORDER BY f.record_id, f.id
                """, rs -> {
                    Map<Long, List<AfterSalesCustomerFollowup>> followups = new HashMap<>();
                    while (rs.next()) {
                        followups.computeIfAbsent(rs.getLong("record_id"), ignored -> new ArrayList<>())
                                .add(new AfterSalesCustomerFollowup(rs.getString("content"),
                                        Instant.ofEpochMilli(rs.getLong("created_at_ms"))));
                    }
                    return followups;
                }, orderNo);
        Map<Long, List<AfterSalesPublicReply>> repliesByRecord = jdbc.query("""
                SELECT e.record_id, e.to_status, e.customer_reply, e.created_at
                  FROM customer_order_after_sales_case_events e
                  JOIN customer_order_after_sales_records r ON r.id = e.record_id
                 WHERE r.order_no = ? AND e.customer_reply IS NOT NULL
                 ORDER BY e.record_id, e.id
                """, rs -> {
                    Map<Long, List<AfterSalesPublicReply>> replies = new HashMap<>();
                    while (rs.next()) {
                        replies.computeIfAbsent(rs.getLong("record_id"), ignored -> new ArrayList<>())
                                .add(new AfterSalesPublicReply(rs.getString("to_status"),
                                        rs.getString("customer_reply"),
                                        rs.getTimestamp("created_at").toInstant()));
                    }
                    return replies;
                }, orderNo);
        return jdbc.query("""
                SELECT r.id, r.category_label, r.content, r.submitted_at_ms,
                       COALESCE(s.status, 'OPEN') AS status, s.updated_at,
                       COALESCE((SELECT MAX(f.id)
                                   FROM customer_order_after_sales_case_followups f
                                  WHERE f.record_id = r.id), 0) AS latest_followup_id,
                       COALESCE(s.handled_through_followup_id, 0) AS handled_through_followup_id
                  FROM customer_order_after_sales_records r
                  LEFT JOIN customer_order_after_sales_case_state s ON s.record_id = r.id
                 WHERE r.order_no = ?
                 ORDER BY r.submitted_at_ms, r.id
                """, (rs, rowNum) -> {
                    long recordId = rs.getLong("id");
                    List<AfterSalesPublicReply> replies = List.copyOf(
                            repliesByRecord.getOrDefault(recordId, List.of()));
                    String latest = replies.isEmpty() ? null : replies.getLast().content();
                    return new AfterSalesCaseStatus(recordId, rs.getString("category_label"),
                            Instant.ofEpochMilli(rs.getLong("submitted_at_ms")),
                            rs.getString("content"), rs.getString("status"), latest,
                            toInstant(rs.getTimestamp("updated_at")), replies,
                            List.copyOf(followupsByRecord.getOrDefault(recordId, List.of())),
                            rs.getLong("latest_followup_id") > rs.getLong("handled_through_followup_id"));
                }, orderNo);
    }

    private static Instant toInstant(Timestamp ts) { return ts == null ? null : ts.toInstant(); }

    private static String quoteStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "NEW" -> "待处理";
            case "CONTACTED" -> "已联系";
            case "QUOTED" -> "已报价";
            case "CONVERTED" -> "已转订单";
            case "CLOSED" -> "已关闭";
            default -> status == null ? "状态未知" : status;
        };
    }

    private static String orderStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "WAITING_CONFIRMATION" -> "等待确认";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "EXCEPTION" -> "异常处理中";
            default -> status == null ? "状态未知" : status;
        };
    }

    private static String afterSalesCategory(String category) {
        return switch (category == null ? "" : category) {
            case "DELIVERY" -> "交付或进度";
            case "CANCELLATION" -> "取消服务";
            case "REFUND_REVIEW" -> "退款核对";
            case "OTHER" -> "其他问题";
            default -> throw AccountException.badRequest("AFTER_SALES_CATEGORY_INVALID", "请选择问题类型");
        };
    }

    public record SupplementRequest(@NotBlank @Size(max = 1200) String content) {}
    public record AfterSalesRequest(@NotBlank String category, @NotBlank @Size(max = 240) String content) {}
    public record AfterSalesFollowupRequest(@NotBlank @Size(max = 240) String content) {}
    public record OkResponse(boolean ok, String message) { static OkResponse of(String message) { return new OkResponse(true, message); } }
    public record SummaryResponse(long quoteTotal, long orderTotal, long activeOrders, long securityEvents,
                                  List<QuoteItem> recentQuotes, List<OrderItem> recentOrders,
                                  List<LoginLogItem> recentLogins) {}
    public record QuoteListResponse(List<QuoteItem> items, long total, int page, int size) {}
    public record OrderListResponse(List<OrderItem> items, long total, int page, int size) {}
    public record OrderDetailResponse(OrderItem order, QuoteItem sourceQuote, List<TimelineItem> timeline,
                                      List<AfterSalesRecord> afterSalesRecords,
                                      List<AfterSalesCaseStatus> afterSalesCases) {}
    public record AfterSalesRecord(String category, Instant submittedAt, String content) {}
    public record AfterSalesCaseStatus(long recordId, String category, Instant submittedAt,
                                       String content, String status, String customerReply,
                                       Instant updatedAt, List<AfterSalesPublicReply> replies,
                                       List<AfterSalesCustomerFollowup> followups,
                                       boolean pendingCustomerFollowup) {}
    public record AfterSalesPublicReply(String status, String content, Instant repliedAt) {}
    public record AfterSalesCustomerFollowup(String content, Instant createdAt) {}
    public record QuoteItem(String businessNo, String game, String demand, String currentProgress,
                            String platform, String expectedTime, String status, String statusText,
                            String customerSupplement, Instant createdAt, Instant updatedAt) {}
    public record OrderItem(String orderNo, String businessNo, String sourceQuoteBusinessNo,
                            String status, String statusText, Integer progressPercent, String progressText,
                            String customerNote, String gameName, String serviceName,
                            Instant createdAt, Instant updatedAt) {}
    public record TimelineItem(String title, String content, Instant createdAt) {}
    public record LoginLogItem(String action, String detail, String ip, String userAgent, Instant createdAt) {}
}

