package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfills the stable guest business identity for anonymous quotes created before the guest quote
 * flow was introduced (or for rare quotes whose browser capability could not be attached).
 *
 * <p>This service is intentionally staff-action only: it never infers a customer account from a
 * callback contact and it never grants a browser session. It only creates the same anonymous
 * business subject that a new guest quote receives at submission time. Cross-browser access still
 * requires the one-time guest link flow.
 */
@Service
public class GuestQuoteAccessBootstrapService {
    private final JdbcTemplate jdbc;

    public GuestQuoteAccessBootstrapService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long ensureAnonymousGuestAccess(String businessNo) {
        String no = normalizeBusinessNo(businessNo);
        List<QuoteAccessState> rows = jdbc.query("""
                SELECT q.id,q.customer_account_id,a.id AS guest_access_id
                  FROM quotes q
                  LEFT JOIN guest_business_access a
                    ON a.quote_id=q.id AND a.status='ACTIVE'
                 WHERE q.business_no=?
                 FOR UPDATE
                """, (rs, rowNum) -> new QuoteAccessState(
                        rs.getString("id"),
                        (Long) rs.getObject("customer_account_id"),
                        (Long) rs.getObject("guest_access_id")), no);
        if (rows.isEmpty()) {
            throw new NotFoundException();
        }

        QuoteAccessState quote = rows.getFirst();
        if (quote.customerAccountId() != null) {
            throw new BadRequestException("该询价已经归属注册账号，请使用账号报价流程");
        }
        if (quote.guestAccessId() != null) {
            return quote.guestAccessId();
        }

        jdbc.update("""
                INSERT IGNORE INTO guest_business_access(
                  quote_id,business_no,status,created_at,updated_at)
                VALUES (?,?,'ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, quote.quoteId(), no);

        List<Long> accessIds = jdbc.query("""
                SELECT id FROM guest_business_access
                 WHERE quote_id=? AND business_no=? AND status='ACTIVE'
                 LIMIT 1
                """, (rs, rowNum) -> rs.getLong(1), quote.quoteId(), no);
        if (accessIds.isEmpty()) {
            throw new IllegalStateException("游客业务凭证补建失败，请刷新后重试");
        }
        return accessIds.getFirst();
    }

    private String normalizeBusinessNo(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) {
            throw new IllegalArgumentException("业务编号不能为空");
        }
        String normalized = businessNo.trim();
        if (normalized.length() > 40) {
            throw new IllegalArgumentException("业务编号过长");
        }
        return normalized;
    }

    private record QuoteAccessState(
            String quoteId,
            Long customerAccountId,
            Long guestAccessId) {}
}
