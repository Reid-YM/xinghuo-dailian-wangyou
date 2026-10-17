package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatically repairs missing order-to-account ownership using trusted identity signals.
 *
 * <p>The service never overwrites an existing owner. It first trusts an account id already
 * recorded on the quote that shares the unified business number. If that lineage is absent,
 * it may resolve the quote's primary contact against a single ACTIVE account whose primary
 * login contact is verified. Auxiliary quote contacts are deliberately not used as identity.
 */
@Service
public class AutomaticOrderAccountLinkService {

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
            Map.entry("电话", Account.ContactType.PHONE),
            Map.entry("email", Account.ContactType.EMAIL),
            Map.entry("mail", Account.ContactType.EMAIL),
            Map.entry("邮箱", Account.ContactType.EMAIL));

    private final JdbcTemplate jdbc;

    public AutomaticOrderAccountLinkService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Reconcile one order for an administrator workflow.
     *
     * <p>This method is intentionally idempotent. Callers must perform their own authorization
     * and business-scope checks before invoking it.
     */
    @Transactional
    public LinkOutcome reconcile(String identifier) {
        String key = identifier == null ? "" : identifier.trim();
        if (key.isEmpty() || key.length() > 80) {
            return LinkOutcome.none();
        }

        List<Candidate> rows = jdbc.query("""
                SELECT o.order_no, o.business_no, o.customer_account_id,
                       q.customer_account_id AS quote_account_id,
                       q.customer_contact, q.contact_method
                  FROM customer_orders o
                  LEFT JOIN quotes q ON q.business_no = o.business_no
                 WHERE o.order_no = ? OR o.business_no = ?
                 LIMIT 2
                """,
                (rs, i) -> new Candidate(
                        rs.getString("order_no"),
                        rs.getString("business_no"),
                        (Long) rs.getObject("customer_account_id"),
                        (Long) rs.getObject("quote_account_id"),
                        rs.getString("customer_contact"),
                        rs.getString("contact_method")),
                key, key);

        if (rows.size() != 1) {
            return LinkOutcome.none();
        }
        Candidate row = rows.getFirst();
        if (row.orderOwnerId() != null) {
            return LinkOutcome.alreadyLinked(row.orderOwnerId());
        }

        Long ownerId = row.quoteOwnerId();
        String source = "QUOTE_ACCOUNT";
        if (ownerId == null) {
            ownerId = verifiedAccountFor(row.contactMethod(), row.contactValue()).orElse(null);
            source = "VERIFIED_PRIMARY_CONTACT";
        }
        if (ownerId == null) {
            return LinkOutcome.none();
        }

        boolean linked = link(row.orderNo(), row.businessNo(), ownerId);
        return linked
                ? new LinkOutcome(true, ownerId, source)
                : LinkOutcome.none();
    }

    /**
     * Reconcile all recent unowned orders that can be proven to belong to the current account.
     *
     * <p>Quote lineage is authoritative even if the account later changes its login contact.
     * Contact matching is only attempted when the account's primary login contact is verified.
     */
    @Transactional
    public int reconcile(Account account) {
        if (account == null || account.getId() == null) {
            return 0;
        }

        List<Candidate> rows = jdbc.query("""
                SELECT o.order_no, o.business_no, o.customer_account_id,
                       q.customer_account_id AS quote_account_id,
                       q.customer_contact, q.contact_method
                  FROM customer_orders o
                  LEFT JOIN quotes q ON q.business_no = o.business_no
                 WHERE o.customer_account_id IS NULL
                 ORDER BY o.created_at DESC
                 LIMIT ?
                """,
                (rs, i) -> new Candidate(
                        rs.getString("order_no"),
                        rs.getString("business_no"),
                        (Long) rs.getObject("customer_account_id"),
                        (Long) rs.getObject("quote_account_id"),
                        rs.getString("customer_contact"),
                        rs.getString("contact_method")),
                CANDIDATE_SCAN_LIMIT);

        int linked = 0;
        for (Candidate row : rows) {
            boolean lineageMatch = account.getId().equals(row.quoteOwnerId());
            boolean contactMatch = account.isContactVerified()
                    && contactMatches(account, row.contactMethod(), row.contactValue());
            if ((lineageMatch || contactMatch)
                    && link(row.orderNo(), row.businessNo(), account.getId())) {
                linked++;
            }
        }
        return linked;
    }

    private boolean link(String orderNo, String businessNo, Long ownerId) {
        int updated = jdbc.update("""
                UPDATE customer_orders
                   SET customer_account_id = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                 WHERE order_no = ? AND customer_account_id IS NULL
                """, ownerId, orderNo);
        if (updated == 0) {
            return false;
        }
        if (businessNo != null && !businessNo.isBlank()) {
            jdbc.update("""
                    UPDATE quotes
                       SET customer_account_id = ?
                     WHERE business_no = ? AND customer_account_id IS NULL
                    """, ownerId, businessNo);
        }
        return true;
    }

    private Optional<Long> verifiedAccountFor(String rawMethod, String rawValue) {
        Optional<Account.ContactType> type = contactType(rawMethod);
        if (type.isEmpty() || rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = ContactNormalizer.normalize(type.get(), rawValue);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        List<Long> ids = jdbc.query("""
                SELECT id
                  FROM customer_accounts
                 WHERE contact_type = ?
                   AND contact_norm = ?
                   AND contact_verified = TRUE
                   AND status = 'ACTIVE'
                   AND deleted_at IS NULL
                 LIMIT 2
                """, (rs, i) -> rs.getLong(1), type.get().name(), normalized);
        return ids.size() == 1 ? Optional.of(ids.getFirst()) : Optional.empty();
    }

    private boolean contactMatches(Account account, String rawMethod, String rawValue) {
        if (rawValue == null || rawValue.isBlank() || account.getContactType() == null) {
            return false;
        }
        Optional<Account.ContactType> type = contactType(rawMethod);
        if (type.isEmpty() || type.get() != account.getContactType()) {
            return false;
        }
        String normalized = ContactNormalizer.normalize(type.get(), rawValue);
        return !normalized.isBlank() && normalized.equals(account.getContactNorm());
    }

    private Optional<Account.ContactType> contactType(String rawMethod) {
        if (rawMethod == null || rawMethod.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(METHOD_ALIASES.get(rawMethod.trim().toLowerCase(Locale.ROOT)));
    }

    private record Candidate(
            String orderNo,
            String businessNo,
            Long orderOwnerId,
            Long quoteOwnerId,
            String contactValue,
            String contactMethod) {
    }

    public record LinkOutcome(boolean linked, Long accountId, String source) {
        static LinkOutcome none() {
            return new LinkOutcome(false, null, null);
        }

        static LinkOutcome alreadyLinked(Long accountId) {
            return new LinkOutcome(false, accountId, "EXISTING");
        }
    }
}
