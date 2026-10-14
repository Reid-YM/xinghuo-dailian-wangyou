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

class XingweiAfterSalesCategorySummaryServiceTest {
    private JdbcTemplate jdbc;
    private XingweiPermissionGuard permissions;
    private XingweiAfterSalesCategorySummaryService categories;
    private Authentication actor;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:xw_case_categories_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE customer_orders (order_no VARCHAR(40) PRIMARY KEY, business_no VARCHAR(20))");
        jdbc.execute("CREATE TABLE customer_order_after_sales_records (id BIGINT PRIMARY KEY, order_no VARCHAR(40), category VARCHAR(24))");
        jdbc.execute("CREATE TABLE xw_business_ownership (business_no VARCHAR(20) PRIMARY KEY, owner_admin_user_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(16))");
        jdbc.update("INSERT INTO customer_orders VALUES ('O1','XW1'),('O2','XW2')");
        jdbc.update("INSERT INTO customer_order_after_sales_records VALUES (1,'O1','DELIVERY'),(2,'O1','REFUND_REVIEW'),(3,'O2','CANCELLATION'),(4,'O2','OTHER')");
        jdbc.update("INSERT INTO xw_departments VALUES (3,'03'),(4,'04')");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XW1',7,3),('XW2',8,4)");

        permissions = mock(XingweiPermissionGuard.class);
        actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        when(permissions.has(actor, "order:view")).thenReturn(true);
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view"), List.of(), "SELF", 7L, "03"));
        categories = new XingweiAfterSalesCategorySummaryService(jdbc, permissions);
    }

    @Test
    void summaryUsesTheSameSelfDepartmentAllAndDeniedScopes() {
        assertEquals(new XingweiAfterSalesCategorySummaryService.CategorySummary(1, 0, 1, 0),
                categories.summary(actor));

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view"), List.of(), "DEPARTMENT", 8L, "04"));
        assertEquals(new XingweiAfterSalesCategorySummaryService.CategorySummary(0, 1, 0, 1),
                categories.summary(actor));

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "LEGACY_ADMIN", true, List.of("order:view"), List.of(), "ALL", 7L, null));
        assertEquals(new XingweiAfterSalesCategorySummaryService.CategorySummary(1, 1, 1, 1),
                categories.summary(actor));

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "DENIED", false, List.of(), List.of(), "ALL", null, null));
        assertEquals(new XingweiAfterSalesCategorySummaryService.CategorySummary(0, 0, 0, 0),
                categories.summary(actor));
    }

    @Test
    void summaryRequiresOrderViewPermission() {
        when(permissions.has(actor, "order:view")).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> categories.summary(actor));
    }
}
