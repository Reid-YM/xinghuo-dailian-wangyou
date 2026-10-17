package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repairs missing order-to-account ownership from authoritative quote lineage only.
 *
 * <p>Customer-entered quote contacts are intentionally not identity evidence. A verified
 * account that happens to own the same phone/QQ/WeChat value does not prove that an anonymous
 * quote was submitted by that account. Automatic linking therefore trusts only a customer
 * account already recorded on the source quote. For legacy orders without source_quote_id we
 * may fall back to the unique unified business number.
 */
@Service
public class AutomaticOrderAccountLinkService {

    private static final int CANDIDATE_SCAN_LIMIT = 500;

    private final JdbcTemplate jdbc;

    public AutomaticOrderAccountLinkService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Reconcile one order for an administrator workflow. */
    @Transactional
    public LinkOutcome reconcile(String identifier) {
        String key = identifier == null ? "" : identifier.trim();
        if (key.isEmpty() || key.length() > 80) {
            return LinkOutcome.none();
        }

        List<Candidate> rows = candidates("WHERE o.order_no = ? OR o.business_no = ? LIMIT 2", key, key);
        if (rows.size() != 1) {
            return LinkOutcome.none();
        }
        Candidate row = rows.getFirst();
        if (row.orderOwnerId() != null) {
            return LinkOutcome.alreadyLinked(row.orderOwnerId());
        }
        if (row.quoteOwnerId() == null || row.lineageSource() == null) {
            return LinkOutcome.none();
        }

        boolean linked = link(row, row.quoteOwnerId());
        return linked
                ? new LinkOutcome(true, row.quoteOwnerId(), row.lineageSource())
                : LinkOutcome.none();
    }

    /**
     * Reconcile recent unowned orders that can be proven to belong to the current account.
     *
     * <p>No contact-value matching is performed here. GET / account order views may call this
     * legacy repair path safely because every write is backed by an existing quote account id.
     */
    @Transactional
    public int reconcile(Account account) {
        if (account == null || account.getId() == null) {
            return 0;
        }

        List<Candidate> rows = candidates(
                "WHERE o.customer_account_id IS NULL ORDER BY o.created_at DESC LIMIT ?",
                CANDIDATE_SCAN_LIMIT);

        int linked = 0;
        for (Candidate row : rows) {
            if (account.getId().equals(row.quoteOwnerId()) && row.lineageSource() != null
                    && link(row, account.getId())) {
                linked++;
            }
        }
        return linked;
    }

    private List<Candidate> candidates(String suffix, Object... args) {
        String sql = """
                SELECT o.order_no, o.business_no, o.source_quote_id, o.customer_account_id,
                       COALESCE(q_source.customer_account_id, q_legacy.customer_account_id) AS quote_account_id,
                       CASE
                         WHEN q_source.id IS NOT NULL THEN 'SOURCE_QUOTE_ACCOUNT'
                         WHEN o.source_quote_id IS NULL AND q_legacy.id IS NOT NULL THEN 'LEGACY_BUSINESS_QUOTE_ACCOUNT'
                         ELSE NULL
                       END AS lineage_source
                  FROM customer_orders o
                  LEFT JOIN quotes q_source ON q_source.id = o.source_quote_id
                  LEFT JOIN quotes q_legacy
                         ON o.source_quote_id IS NULL AND q_legacy.business_no = o.business_no
                """ + suffix;
        return jdbc.query(sql,
                (rs, i) -> new Candidate(
                        rs.getString("order_no"),
                        rs.getString("business_no"),
                        rs.getString("source_quote_id"),
                        (Long) rs.getObject("customer_account_id"),
                        (Long) rs.getObject("quote_account_id"),
                        rs.getString("lineage_source")),
                args);
    }

    private boolean link(Candidate row, Long ownerId) {
        int updated = jdbc.update("""
                UPDATE customer_orders
                   SET customer_account_id = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                 WHERE order_no = ? AND customer_account_id IS NULL
                """, ownerId, row.orderNo());
        if (updated == 0) {
            return false;
        }

        if (row.sourceQuoteId() != null && !row.sourceQuoteId().isBlank()) {
            jdbc.update("""
                    UPDATE quotes
                       SET customer_account_id = ?
                     WHERE id = ? AND customer_account_id IS NULL
                    """, ownerId, row.sourceQuoteId());
        } else if (row.businessNo() != null && !row.businessNo().isBlank()) {
            jdbc.update("""
                    UPDATE quotes
                       SET customer_account_id = ?
                     WHERE business_no = ? AND customer_account_id IS NULL
                    """, ownerId, row.businessNo());
        }
        return true;
    }

    private record Candidate(
            String orderNo,
            String businessNo,
            String sourceQuoteId,
            Long orderOwnerId,
            Long quoteOwnerId,
            String lineageSource) {
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
