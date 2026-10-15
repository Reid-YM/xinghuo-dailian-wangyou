package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class XingweiAfterSalesAttentionAgeSummaryServiceTest {
    private JdbcTemplate jdbc;
    private XingweiPermissionGuard permissions;
    private XingweiAfterSalesAttentionAgeSummaryService aging;
    private Authentication actor;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:xw_case_attention_age_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE customer_orders (order_no VARCHAR(40) PRIMARY KEY, business_no VARCHAR(20))");
        jdbc.execute("CREATE TABLE customer_order_after_sales_records (id BIGINT PRIMARY KEY, order_no VARCHAR(40), submitted_at_ms BIGINT)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_state (record_id BIGINT PRIMARY KEY, status VARCHAR(24), handled_through_followup_id BIGINT)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_followups (id BIGINT PRIMARY KEY, record_id BIGINT, created_at_ms BIGINT)");
        jdbc.execute("CREATE TABLE xw_business_ownership (business_no VARCHAR(20) PRIMARY KEY, owner_admin_user_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(16))");

        long now = System.currentTimeMillis();
        jdbc.update("INSERT INTO customer_orders VALUES ('O1','XW1'),('O2','XW2')");
        jdbc.update("INSERT INTO xw_departments VALUES (3,'03'),(4,'04')");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XW1',7,3),('XW2',8,4)");
        jdbc.update("INSERT INTO customer_order_after_sales_records VALUES (1,'O1',?),(2,'O1',?),(3,'O1',?),(4,'O1',?),(5,'O2',?)",
                now - 2L * 60 * 60 * 1000,
                now - 10L * 60 * 60 * 1000,
                now - 10L * 60 * 60 * 1000,
                now - 10L * 60 * 60 * 1000,
                now - 80L * 60 * 60 * 1000);
        jdbc.update("INSERT INTO customer_order_after_sales_case_state VALUES (2,'IN_PROGRESS',0),(3,'RESOLVED',0),(4,'IN_PROGRESS',0)");
        jdbc.update("INSERT INTO customer_order_after_sales_case_followups VALUES (20,2,?),(30,3,?)",
                now - 30L * 60 * 60 * 1000,
                now - 100L * 60 * 60 * 1000);

        permissions = mock(XingweiPermissionGuard.class);
        actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        when(permissions.has(actor, "order:view")).thenReturn(true);
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view"), List.of(), "SELF", 7L, "03"));
        aging = new XingweiAfterSalesAttentionAgeSummaryService(jdbc, permissions);
    }

    @Test
    void summaryPartitionsOnlyRecordsThatNeedAttentionAcrossScopes() {
        assertEquals(new XingweiAfterSalesAttentionAgeSummaryService.AttentionAgeSummary(3, 1, 1, 1),
                aging.summary(actor));

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view"), List.of(), "DEPARTMENT", 8L, "04"));
        assertEquals(new XingweiAfterSalesAttentionAgeSummaryService.AttentionAgeSummary(1, 0, 0, 1),
                aging.summary(actor));

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "LEGACY_ADMIN", true, List.of("order:view"), List.of(), "ALL", 7L, null));
        assertEquals(new XingweiAfterSalesAttentionAgeSummaryService.AttentionAgeSummary(4, 1, 1, 2),
                aging.summary(actor));

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "DENIED", false, List.of(), List.of(), "ALL", null, null));
        assertEquals(new XingweiAfterSalesAttentionAgeSummaryService.AttentionAgeSummary(0, 0, 0, 0),
                aging.summary(actor));
    }

    @Test
    void summaryRequiresOrderViewPermission() {
        when(permissions.has(actor, "order:view")).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> aging.summary(actor));
    }
}
