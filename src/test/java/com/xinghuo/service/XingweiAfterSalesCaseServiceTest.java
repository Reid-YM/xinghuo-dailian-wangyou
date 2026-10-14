package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.springframework.web.server.ResponseStatusException;

class XingweiAfterSalesCaseServiceTest {
    private JdbcTemplate jdbc;
    private XingweiPermissionGuard permissions;
    private XingweiBusinessScopeService scope;
    private AuditService audit;
    private XingweiAfterSalesCaseService cases;
    private Authentication actor;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:xw_cases_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE customer_orders (order_no VARCHAR(40) PRIMARY KEY, business_no VARCHAR(20))");
        jdbc.execute("CREATE TABLE customer_order_after_sales_records (id BIGINT PRIMARY KEY, order_no VARCHAR(40), category VARCHAR(24), category_label VARCHAR(32), content VARCHAR(240), submitted_at_ms BIGINT)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_state (record_id BIGINT PRIMARY KEY, status VARCHAR(24), handling_note VARCHAR(600), handled_by_admin_user_id BIGINT, updated_at TIMESTAMP, version BIGINT)");
        jdbc.update("INSERT INTO customer_orders VALUES ('O1','XW1'),('O2','XW2')");
        jdbc.update("INSERT INTO customer_order_after_sales_records VALUES (1,'O1','PROGRESS','进度问题','请核对进度',1000),(2,'O2','OTHER','其他','跨范围记录',2000)");
        permissions = mock(XingweiPermissionGuard.class);
        scope = mock(XingweiBusinessScopeService.class);
        audit = mock(AuditService.class);
        actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        when(permissions.has(actor, "order:view")).thenReturn(true);
        when(permissions.has(actor, "order:manage")).thenReturn(true);
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view", "order:manage"), List.of(), "SELF", 7L, "03"));
        when(scope.canAccessBusiness(actor, "XW1")).thenReturn(true);
        cases = new XingweiAfterSalesCaseService(jdbc, permissions, scope, audit);
    }

    @Test
    void scopedWorkflowStartsOpenAndAuditsVersionedTransitions() {
        assertEquals("OPEN", cases.records("XW1", actor).getFirst().status());
        assertEquals(0, cases.records("XW1", actor).getFirst().version());

        var inProgress = cases.update("XW1", 1, "IN_PROGRESS", "已联系客户", 0, actor, null);
        assertEquals("IN_PROGRESS", inProgress.status());
        assertEquals(1, inProgress.version());
        var resolved = cases.update("XW1", 1, "RESOLVED", "已核实并处理", 1, actor, null);
        assertEquals("RESOLVED", resolved.status());
        assertEquals(2, resolved.version());
        verify(audit).log(eq("XW_AFTER_SALES_CASE_UPDATE"), eq("AFTER_SALES_RECORD"), eq("1"),
                eq("staff"), any(), any(), eq("已核实并处理"), eq(null));
    }

    @Test
    void staleVersionAndCrossOrderRecordCannotOverwrite() {
        cases.update("XW1", 1, "IN_PROGRESS", "开始处理", 0, actor, null);
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> cases.update("XW1", 1, "RESOLVED", "过期更新", 0, actor, null)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> cases.update("XW1", 2, "RESOLVED", "跨订单", 0, actor, null)).getStatusCode().value());
        assertEquals("IN_PROGRESS", cases.records("XW1", actor).getFirst().status());
    }

    @Test
    void permissionAndOwnershipBothGuardReadsAndMutations() {
        assertThrows(AccessDeniedException.class, () -> cases.records("XW2", actor));
        assertThrows(AccessDeniedException.class,
                () -> cases.update("XW2", 2, "RESOLVED", "无权限", 0, actor, null));
        when(permissions.has(actor, "order:manage")).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> cases.update("XW1", 1, "RESOLVED", "无权限", 0, actor, null));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM customer_order_after_sales_case_state", Integer.class));
    }
}
