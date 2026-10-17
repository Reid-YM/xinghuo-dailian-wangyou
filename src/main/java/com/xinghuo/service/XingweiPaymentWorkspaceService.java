package com.xinghuo.service;

import com.xinghuo.account.service.AutomaticOrderAccountLinkService;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.payment.GuestWeChatPaymentService;
import com.xinghuo.payment.WeChatNativePaymentService;
import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Native payment administration keeps provider switches and business ownership authoritative. */
@Service
public class XingweiPaymentWorkspaceService {
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard guard;
    private final XingweiBusinessScopeService business;
    private final ObjectProvider<WeChatNativePaymentService> provider;
    private final ObjectProvider<GuestWeChatPaymentService> guestProvider;
    private final AuditService audits;
    private final AutomaticOrderAccountLinkService accountLinks;

    /** Kept for focused unit tests that do not exercise guest payment creation. */
    public XingweiPaymentWorkspaceService(
            JdbcTemplate jdbc,
            XingweiPermissionGuard guard,
            XingweiBusinessScopeService business,
            ObjectProvider<WeChatNativePaymentService> provider,
            AuditService audits,
            AutomaticOrderAccountLinkService accountLinks) {
        this(jdbc, guard, business, provider, null, audits, accountLinks);
    }

    @Autowired
    public XingweiPaymentWorkspaceService(
            JdbcTemplate jdbc,
            XingweiPermissionGuard guard,
            XingweiBusinessScopeService business,
            ObjectProvider<WeChatNativePaymentService> provider,
            ObjectProvider<GuestWeChatPaymentService> guestProvider,
            AuditService audits,
            AutomaticOrderAccountLinkService accountLinks) {
        this.jdbc = jdbc;
        this.guard = guard;
        this.business = business;
        this.provider = provider;
        this.guestProvider = guestProvider;
        this.audits = audits;
        this.accountLinks = accountLinks;
    }

    private void require(Authentication actor, String code) {
        if (!guard.has(actor, code)) {
            throw new AccessDeniedException("没有对应的支付操作权限");
        }
    }

    private void scope(Authentication actor, String no) {
        if (!business.canAccessBusiness(actor, no)) {
            throw new AccessDeniedException("支付业务不在可见数据范围内");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot(Authentication actor, String q, int page) {
        require(actor, "payment:view");
        if (page < 0 || page > 10000) {
            throw new BadRequestException("页码超出范围");
        }
        String query = q == null ? "" : q.trim();
        if (query.length() > 100) {
            throw new BadRequestException("关键词不能超过100字");
        }
        var clause = business.queryScope(actor);
        var args = new ArrayList<Object>(clause.args());
        String where = "p.provider='WECHAT' AND (" + clause.sql() + ")";
        if (!query.isEmpty()) {
            String value = "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            where += " AND (p.payment_no LIKE ? ESCAPE '!' OR p.business_no LIKE ? ESCAPE '!' OR p.subject LIKE ? ESCAPE '!')";
            args.addAll(List.of(value, value, value));
        }
        String from = " FROM payment_order p LEFT JOIN xw_business_ownership o ON o.business_no=p.business_no LEFT JOIN xw_departments d ON d.id=o.department_id WHERE " + where;
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + from, Long.class, args.toArray());
        args.add(25);
        args.add(page * 25);
        var items = jdbc.query(
                "SELECT p.payment_no,p.business_no,p.subject,p.amount,p.currency,p.status,p.created_at,p.paid_at,p.updated_at" + from + " ORDER BY p.id DESC LIMIT ? OFFSET ?",
                (rs, n) -> {
                    var row = new LinkedHashMap<String, Object>();
                    row.put("paymentNo", rs.getString(1));
                    row.put("businessNo", rs.getString(2));
                    row.put("subject", rs.getString(3));
                    row.put("amount", rs.getBigDecimal(4));
                    row.put("currency", rs.getString(5));
                    row.put("status", rs.getString(6));
                    row.put("createdAt", rs.getTimestamp(7));
                    row.put("paidAt", rs.getTimestamp(8));
                    row.put("revision", revision("payment", rs.getString(1), rs.getTimestamp(9)));
                    return row;
                },
                args.toArray());
        return Map.of(
                "items", items,
                "total", total == null ? 0 : total,
                "page", page,
                "size", 25,
                "enabled", provider.getIfAvailable() != null,
                "canManage", guard.has(actor, "payment:manage"));
    }

    @Transactional
    public Map<String, Object> order(String identifier, Authentication actor) {
        require(actor, "payment:view");
        Map<String, Object> row = orderState(identifier, actor, false, null);
        if (!hasCustomerSubject(row)) {
            var outcome = accountLinks.reconcile(identifier);
            if (outcome.linked()) {
                auditAutoLink(row, outcome, actor);
                row = orderState(identifier, actor, false, null);
                row.put("customerAutoLinked", true);
            }
        }
        return row;
    }

    private Map<String, Object> orderState(
            String identifier,
            Authentication actor,
            boolean lock,
            String expected) {
        if (identifier == null || identifier.isBlank() || identifier.length() > 80) {
            throw new BadRequestException("请填写有效订单号或业务编号");
        }
        var rows = jdbc.query(
                "SELECT order_no,COALESCE(business_no,order_no),status,customer_account_id,guest_access_id,updated_at,business_no FROM customer_orders WHERE order_no=? OR business_no=?" + (lock ? " FOR UPDATE" : ""),
                (rs, n) -> {
                    var row = new LinkedHashMap<String, Object>();
                    boolean accountBound = rs.getObject(4) != null;
                    Long guestAccessId = (Long) rs.getObject(5);
                    boolean guestBound = guestAccessId != null;
                    String rawBusinessNo = rs.getString(7);
                    row.put("orderNo", rs.getString(1));
                    row.put("businessNo", rs.getString(2));
                    row.put("status", rs.getString(3));
                    row.put("customerBound", accountBound);
                    row.put("guestBound", guestBound);
                    row.put("customerType", accountBound ? "ACCOUNT" : guestBound ? "GUEST" : "UNBOUND");
                    row.put("paymentEligible", (accountBound || guestBound)
                            && rawBusinessNo != null && !rawBusinessNo.isBlank());
                    row.put("revision", revision("order", rs.getString(1), rs.getTimestamp(6)));
                    if (lock && guestAccessId != null) {
                        row.put("guestAccessId", guestAccessId);
                    }
                    return row;
                },
                identifier.trim(), identifier.trim());
        if (rows.isEmpty()) {
            throw new NotFoundException();
        }
        if (rows.size() != 1) {
            throw new BadRequestException("编号对应多个订单，请使用完整订单号");
        }
        var row = rows.getFirst();
        scope(actor, String.valueOf(row.get("businessNo")));
        if (lock) {
            match(String.valueOf(row.get("revision")), expected);
        }
        return row;
    }

    @Transactional
    public Map<String, Object> prepare(
            String identifier,
            PrepareInput body,
            String expected,
            Authentication actor,
            HttpServletRequest request) {
        require(actor, "payment:manage");

        Map<String, Object> preview = orderState(identifier, actor, false, null);
        if (!hasCustomerSubject(preview)) {
            var outcome = accountLinks.reconcile(identifier);
            if (outcome.linked()) {
                auditAutoLink(preview, outcome, actor);
            }
        }

        var row = orderState(identifier, actor, true, expected);
        if (!Boolean.TRUE.equals(row.get("paymentEligible"))) {
            throw new BadRequestException("订单尚未建立可支付客户归属；游客订单需先通过正式报价链接确认，注册用户订单需绑定账号");
        }

        WeChatNativePaymentService.Payment result;
        if (Boolean.TRUE.equals(row.get("guestBound"))) {
            GuestWeChatPaymentService guestPayments = enabledGuest();
            result = guestPayments.prepare(
                    String.valueOf(row.get("orderNo")),
                    ((Number) row.get("guestAccessId")).longValue(),
                    body.amount(),
                    body.subject());
        } else {
            result = enabled().prepare(String.valueOf(row.get("orderNo")), body.amount(), body.subject());
        }
        audits.log(
                "XW_PAYMENT_PREPARE",
                "PAYMENT",
                result.paymentNo(),
                actor.getName(),
                null,
                Map.of(
                        "businessNo", result.businessNo(),
                        "amount", result.amount(),
                        "status", result.status(),
                        "customerType", row.get("customerType")),
                body.reason(),
                request);
        return publicPayment(result);
    }

    @Transactional
    public Map<String, Object> close(
            String paymentNo,
            String expected,
            Authentication actor,
            HttpServletRequest request) {
        require(actor, "payment:manage");
        var rows = jdbc.query(
                "SELECT business_no,status,updated_at FROM payment_order WHERE payment_no=? AND provider='WECHAT' FOR UPDATE",
                (rs, n) -> Map.<String, Object>of(
                        "businessNo", rs.getString(1),
                        "status", rs.getString(2),
                        "revision", revision("payment", paymentNo, rs.getTimestamp(3))),
                paymentNo);
        if (rows.isEmpty()) {
            throw new NotFoundException();
        }
        var row = rows.getFirst();
        scope(actor, String.valueOf(row.get("businessNo")));
        match(String.valueOf(row.get("revision")), expected);
        var payments = enabled();
        audits.log(
                "XW_PAYMENT_CLOSE_REQUEST",
                "PAYMENT",
                paymentNo,
                actor.getName(),
                Map.of("status", row.get("status")),
                Map.of("requested", true),
                "管理员确认关闭待支付单",
                request);
        return Map.of("status", payments.close(paymentNo) ? "CANCELLED" : "CAPTURED");
    }

    private boolean hasCustomerSubject(Map<String, Object> row) {
        return Boolean.TRUE.equals(row.get("customerBound")) || Boolean.TRUE.equals(row.get("guestBound"));
    }

    private void auditAutoLink(
            Map<String, Object> row,
            AutomaticOrderAccountLinkService.LinkOutcome outcome,
            Authentication actor) {
        audits.log(
                "XW_ORDER_ACCOUNT_AUTO_LINK",
                "ORDER",
                String.valueOf(row.get("businessNo")),
                actor.getName(),
                Map.of("customerBound", false),
                Map.of("customerBound", true, "source", outcome.source()),
                "支付工作台按来源询价自动恢复客户账号归属",
                null);
    }

    private WeChatNativePaymentService enabled() {
        var service = provider.getIfAvailable();
        if (service == null) {
            throw new BadRequestException("真实收款开关未开启，此处只能查看已有记录");
        }
        return service;
    }

    private GuestWeChatPaymentService enabledGuest() {
        var service = guestProvider == null ? null : guestProvider.getIfAvailable();
        if (service == null) {
            throw new BadRequestException("真实收款开关未开启，此处只能查看已有记录");
        }
        return service;
    }

    private Map<String, Object> publicPayment(WeChatNativePaymentService.Payment p) {
        return Map.of(
                "paymentNo", p.paymentNo(),
                "businessNo", p.businessNo(),
                "subject", p.subject(),
                "amount", p.amount(),
                "status", p.status());
    }

    private void match(String actual, String expected) {
        if (expected == null || (!actual.equals(expected) && !("\"" + actual + "\"").equals(expected))) {
            throw new OptimisticLockingFailureException("记录已变化，请重新读取核对后再操作");
        }
    }

    private String revision(String kind, String no, Object updated) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((kind + ":" + no + ":" + updated).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record PrepareInput(
            @NotNull @DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer = 7, fraction = 2) BigDecimal amount,
            @NotBlank @Size(max = 127) String subject,
            @NotBlank @Size(max = 500) String reason) {
    }
}
