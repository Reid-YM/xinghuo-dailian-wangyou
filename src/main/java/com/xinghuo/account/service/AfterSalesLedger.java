package com.xinghuo.account.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Reads new ledger rows alongside older after-sales blocks kept in order notes. */
public final class AfterSalesLedger {
    private AfterSalesLedger() {}

    public static List<AfterSalesNoteParser.AfterSalesRecord> recordsForOrder(
            JdbcTemplate jdbc, String orderNo, String customerNote) {
        if (orderNo == null) return AfterSalesNoteParser.parse(customerNote);
        List<AfterSalesNoteParser.AfterSalesRecord> stored = jdbc.query("""
                SELECT category_label, content, submitted_at_ms
                  FROM customer_order_after_sales_records
                 WHERE order_no = ?
                 ORDER BY submitted_at_ms, id
                """, (rs, rowNum) -> new AfterSalesNoteParser.AfterSalesRecord(
                rs.getString("category_label"), Instant.ofEpochMilli(rs.getLong("submitted_at_ms")),
                rs.getString("content")), orderNo);
        return combine(customerNote, stored);
    }

    public static Map<String, List<AfterSalesNoteParser.AfterSalesRecord>> recordsForOrders(
            JdbcTemplate jdbc, List<String> orderNos) {
        if (orderNos.isEmpty()) return Map.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(orderNos.size(), "?"));
        Map<String, List<AfterSalesNoteParser.AfterSalesRecord>> byOrder = new HashMap<>();
        jdbc.query("""
                SELECT order_no, category_label, content, submitted_at_ms
                  FROM customer_order_after_sales_records
                 WHERE order_no IN (%s)
                 ORDER BY order_no, submitted_at_ms, id
                """.formatted(placeholders), rs -> {
            byOrder.computeIfAbsent(rs.getString("order_no"), ignored -> new ArrayList<>())
                    .add(new AfterSalesNoteParser.AfterSalesRecord(
                            rs.getString("category_label"), Instant.ofEpochMilli(rs.getLong("submitted_at_ms")),
                            rs.getString("content")));
        }, orderNos.toArray());
        return byOrder;
    }

    public static List<AfterSalesNoteParser.AfterSalesRecord> combine(
            String customerNote, List<AfterSalesNoteParser.AfterSalesRecord> stored) {
        List<AfterSalesNoteParser.AfterSalesRecord> legacy = new ArrayList<>(AfterSalesNoteParser.parse(customerNote));
        for (AfterSalesNoteParser.AfterSalesRecord record : stored) {
            // New rows are also appended to the old note when capacity allows, for rollback compatibility.
            legacy.remove(record);
        }
        List<AfterSalesNoteParser.AfterSalesRecord> records = new ArrayList<>(legacy);
        records.addAll(stored);
        return List.copyOf(records);
    }
}
