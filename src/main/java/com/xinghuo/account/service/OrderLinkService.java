package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单归属：把 customer_orders / quotes 关联到登录账号。
 *
 * <p>直接用 JdbcTemplate 而不是基线实体：本模块刻意不依赖 {@code com.xinghuo.model.*}，
 * 这样既能覆盖进基线 jar 编译，也能脱离基线单独编译校验。
 *
 * <p>归属的唯一真相是既有的 {@code customer_orders.customer_account_id}
 * （V33 建立、V34 marketplace 也在用），不另建关联表。认领的来源与时间写审计日志。
 */
@Service
public class OrderLinkService {

    /**
     * 候选订单扫描上限。
     *
     * <p>报价表里的联系方式是用户手填的原始值（可能是「微信」「+86 138…」等任意写法），
     * 无法在 SQL 层做规范化比对，只能在 Java 侧过滤，因此必须有界。
     * 当前未归属订单量级极小，500 条远超实际需要。
     */
    private static final int CANDIDATE_SCAN_LIMIT = 500;

    private static final Map<String, Account.ContactType> METHOD_ALIASES = Map.ofEntries(
            Map.entry("wechat", Account.ContactType.WECHAT),
            Map.entry("微信", Account.ContactType.WECHAT),
            Map.entry("wx", Account.ContactType.WECHAT),
            Map.entry("qq", Account.ContactType.QQ),
            Map.entry("phone", Account.ContactType.PHONE),
            Map.entry("mobile", Account.ContactType.PHONE),
            Map.entry("手机", Account.ContactType.PHONE),
            Map.entry("手机号", Account.ContactType.PHONE),
            Map.entry("电话", Account.ContactType.PHONE));

    private static final Map<String, String> ORDER_STATUS_TEXT = Map.of(
            "WAITING_CONFIRMATION", "等待确认",
            "IN_PROGRESS", "进行中",
            "COMPLETED", "已完成",
            "EXCEPTION", "异常处理中");

    private final JdbcTemplate jdbc;

    public OrderLinkService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ 列表

    @Transactional(readOnly = true)
    public long countOrders(Long userId) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_orders o WHERE " + TrustedOrderVisibilitySql.PREDICATE,
                Long.class, userId, userId);
        return total == null ? 0 : total;
    }

    @Transactional(readOnly = true)
    public List<OrderRow> listOrders(Long userId, int page, int size) {
        return jdbc.query("""
                SELECT o.order_no, o.business_no, o.status, o.progress_percent, o.progress_text,
                       o.created_at, o.updated_at,
                       q.game AS game_name, q.demand AS service_name
                  FROM customer_orders o
                  LEFT JOIN quotes q ON q.id = o.source_quote_id
                 WHERE %s
                 ORDER BY o.created_at DESC
                 LIMIT ? OFFSET ?
                """.formatted(TrustedOrderVisibilitySql.PREDICATE),
                (rs, i) -> new OrderRow(
                        rs.getString("order_no"),
                        rs.getString("business_no"),
                        rs.getString("status"),
                        statusText(rs.getString("status")),
                        (Integer) rs.getObject("progress_percent"),
                        rs.getString("progress_text"),
                        rs.getString("game_name"),
                        rs.getString("service_name"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))),
                userId, userId, size, (long) page * size);
    }

    // ------------------------------------------------------------------ 候选

    /**
     * 可认领的历史订单：尚未归属、且联系方式与当前账号一致。
     *
     * <p>候选列表只做「缩小范围」，真正的归属判定在 {@link #claim} 里重做一遍 ——
     * 用户在候选页面上停留期间数据可能变化，不能信任客户端回传的判断结果。
     */
    @Transactional(readOnly = true)
    public List<OrderRow> listCandidates(Account account) {
        // 联系方式尚未完成所有权验证时，绝不能用它推导历史订单候选。
        // 微信号、QQ 和手机号都是注册时由客户端填写的自由文本；仅做格式校验
        // 不代表注册者拥有该联系方式。候选列表本身也会泄露订单存在性。
        if (account == null || !account.isContactVerified()) {
            return List.of();
        }
        List<ClaimableRow> raw = jdbc.query("""
                SELECT o.order_no, o.business_no, o.created_at,
                       q.game AS game_name, q.demand AS service_name,
                       q.customer_contact, q.contact_method
                  FROM customer_orders o
                  LEFT JOIN quotes q ON q.id = o.source_quote_id
                 WHERE o.customer_account_id IS NULL
                   AND o.guest_access_id IS NULL
                   AND q.customer_contact IS NOT NULL
                 ORDER BY o.created_at DESC
                 LIMIT ?
                """,
                (rs, i) -> new ClaimableRow(
                        rs.getString("order_no"),
                        rs.getString("business_no"),
                        rs.getString("game_name"),
                        rs.getString("service_name"),
                        rs.getString("customer_contact"),
                        rs.getString("contact_method"),
                        null,
                        toInstant(rs.getTimestamp("created_at")),
                        false),
                CANDIDATE_SCAN_LIMIT);

        List<OrderRow> out = new ArrayList<>();
        for (ClaimableRow row : raw) {
            if (contactMatches(account, row.contactMethod(), row.contactValue())) {
                out.add(new OrderRow(row.orderNo(), row.businessNo(), null, null, null, null,
                        row.gameName(), row.serviceName(), row.createdAt(), null));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ 认领

    /**
     * 认领一条历史订单。
     *
     * <p>判定顺序：订单存在 → 未被他人归属 → 联系方式匹配。
     * 任何一步失败都返回同一类错误，不区分「不存在」与「不匹配」，
     * 避免被用来枚举订单号是否存在。
     */
    @Transactional
    public OrderRow claim(Account account, String orderNoInput) {
        if (account == null || !account.isContactVerified()) {
            throw AccountException.forbidden("CONTACT_VERIFICATION_REQUIRED",
                    "为保护历史订单隐私，请联系客服完成身份核验后归档订单");
        }
        String key = orderNoInput == null ? "" : orderNoInput.trim();
        if (key.isEmpty() || key.length() > 40) {
            throw notClaimable();
        }

        List<ClaimableRow> found = jdbc.query("""
                SELECT o.order_no, o.business_no, o.created_at, o.customer_account_id,
                       o.guest_access_id,
                       q.game AS game_name, q.demand AS service_name,
                       q.customer_contact, q.contact_method
                  FROM customer_orders o
                  LEFT JOIN quotes q ON q.id = o.source_quote_id
                 WHERE o.order_no = ? OR o.business_no = ?
                 LIMIT 1
                """,
                (rs, i) -> new ClaimableRow(
                        rs.getString("order_no"),
                        rs.getString("business_no"),
                        rs.getString("game_name"),
                        rs.getString("service_name"),
                        rs.getString("customer_contact"),
                        rs.getString("contact_method"),
                        (Long) rs.getObject("customer_account_id"),
                        toInstant(rs.getTimestamp("created_at")),
                        rs.getObject("guest_access_id") != null),
                key, key);

        if (found.isEmpty()) {
            throw notClaimable();
        }
        ClaimableRow row = found.get(0);

        if (row.ownerId() != null || row.guestOwned()) {
            // 账号或游客凭证已经拥有该单时不得通过联系方式重复认领。
            throw AccountException.conflict("ORDER_ALREADY_LINKED",
                    "该订单已归属账号，如需变更请联系客服");
        }
        if (!contactMatches(account, row.contactMethod(), row.contactValue())) {
            throw notClaimable();
        }

        int updated = jdbc.update(
                "UPDATE customer_orders SET customer_account_id = ?, version = version + 1 WHERE order_no = ? "
                        + "AND customer_account_id IS NULL AND guest_access_id IS NULL",
                account.getId(), row.orderNo());
        if (updated == 0) {
            // 并发下被别人抢先，或已被后台绑定。
            throw AccountException.conflict("ORDER_ALREADY_LINKED",
                    "该订单已归属账号，如需变更请联系客服");
        }
        // 同一报价下的业务也一并归属，否则用户在「我的订单」里看不到它的来源询价。
        jdbc.update("""
                UPDATE quotes SET customer_account_id = ?
                 WHERE id = (SELECT source_quote_id FROM customer_orders WHERE order_no = ?)
                   AND customer_account_id IS NULL
                """, account.getId(), row.orderNo());

        return new OrderRow(row.orderNo(), row.businessNo(), null, null, null, null,
                row.gameName(), row.serviceName(), row.createdAt(), null);
    }

    // ------------------------------------------------------------------ 工具

    private AccountException notClaimable() {
        return AccountException.badRequest("ORDER_NOT_CLAIMABLE",
                "无法认领该订单，请核对订单号或联系客服");
    }

    /** 报价里的联系方式是自由文本，先按别名映射类型，再走统一规范化。 */
    private boolean contactMatches(Account account, String rawMethod, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return false;
        }
        Account.ContactType type = guessType(rawMethod, account.getContactType());
        if (type != account.getContactType()) {
            return false;
        }
        String normalized = ContactNormalizer.normalize(type, rawValue);
        return !normalized.isEmpty() && normalized.equals(account.getContactNorm());
    }

    private Account.ContactType guessType(String rawMethod, Account.ContactType fallback) {
        if (rawMethod == null || rawMethod.isBlank()) {
            return fallback;
        }
        return METHOD_ALIASES.getOrDefault(rawMethod.trim().toLowerCase(Locale.ROOT), fallback);
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    private static String statusText(String status) {
        if (status == null) {
            return "状态未知";
        }
        return ORDER_STATUS_TEXT.getOrDefault(status, status);
    }

    // ------------------------------------------------------------------ 载体

    /** 订单展示行。 */
    public record OrderRow(
            String orderNo,
            String businessNo,
            String status,
            String statusText,
            Integer progressPercent,
            String progressText,
            String gameName,
            String serviceName,
            Instant createdAt,
            Instant updatedAt) {
    }

    private record ClaimableRow(
            String orderNo,
            String businessNo,
            String gameName,
            String serviceName,
            String contactValue,
            String contactMethod,
            Long ownerId,
            Instant createdAt,
            boolean guestOwned) {
    }
}

