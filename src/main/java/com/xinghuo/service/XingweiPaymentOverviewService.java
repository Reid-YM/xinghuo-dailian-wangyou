package com.xinghuo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only operational view; no payment provider calls or raw callback data. */
@Service
public class XingweiPaymentOverviewService {
    private final JdbcTemplate jdbc;

    public XingweiPaymentOverviewService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        Counts counts = jdbc.queryForObject("""
                SELECT COUNT(*),
                       COALESCE(SUM(CASE WHEN status IN ('CREATED','APPROVED') THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN status = 'CAPTURED' THEN 1 ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END),0)
                  FROM payment_order WHERE provider = 'WECHAT'
                """, (rs, row) -> new Counts(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)));
        List<PaymentRow> recent = jdbc.query("""
                SELECT payment_no,business_no,subject,amount,currency,status,created_at,paid_at
                  FROM payment_order WHERE provider = 'WECHAT'
                 ORDER BY id DESC LIMIT 20
                """, (rs, row) -> new PaymentRow(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getBigDecimal(4), rs.getString(5), rs.getString(6),
                        rs.getObject(7, LocalDateTime.class), rs.getObject(8, LocalDateTime.class)));
        return new Overview(counts, recent);
    }

    public record Counts(long total, long pending, long captured, long cancelled) {}
    public record PaymentRow(String paymentNo, String businessNo, String subject,
                             BigDecimal amount, String currency, String status,
                             LocalDateTime createdAt, LocalDateTime paidAt) {}
    public record Overview(Counts counts, List<PaymentRow> recent) {}
}
