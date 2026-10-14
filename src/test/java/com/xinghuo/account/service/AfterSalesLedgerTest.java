package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AfterSalesLedgerTest {
    @Test
    void batchReadKeepsEachOrdersPrivateHistorySeparate() {
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:after_sales_ledger_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("""
                CREATE TABLE customer_order_after_sales_records (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(40),
                    category_label VARCHAR(32), content VARCHAR(240), submitted_at_ms BIGINT)
                """);
        jdbc.update("INSERT INTO customer_order_after_sales_records(order_no,category_label,content,submitted_at_ms)"
                + " VALUES(?,?,?,?)", "ORD-1", "退款核对", "只属于订单一", 1791972000000L);
        jdbc.update("INSERT INTO customer_order_after_sales_records(order_no,category_label,content,submitted_at_ms)"
                + " VALUES(?,?,?,?)", "ORD-2", "其他问题", "只属于订单二", 1791972001000L);

        var records = AfterSalesLedger.recordsForOrders(jdbc, List.of("ORD-1", "ORD-2"));

        assertEquals("只属于订单一", records.get("ORD-1").getFirst().content());
        assertEquals("只属于订单二", records.get("ORD-2").getFirst().content());
        assertTrue(AfterSalesLedger.recordsForOrders(jdbc, List.of("ORD-3")).isEmpty());
    }
}
