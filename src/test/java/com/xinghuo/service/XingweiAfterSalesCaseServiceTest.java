package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_state (record_id BIGINT PRIMARY KEY, status VARCHAR(24), handling_note VARCHAR(600), handled_by_admin_user_id BIGINT, handled_through_followup_id BIGINT NOT NULL DEFAULT 0, updated_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_events (id BIGINT AUTO_INCREMENT PRIMARY KEY, record_id BIGINT, from_status VARCHAR(24), to_status VARCHAR(24), handling_note VARCHAR(600), customer_reply VARCHAR(600), handled_by_admin_user_id BIGINT, created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_followups (id BIGINT AUTO_INCREMENT PRIMARY KEY, record_id BIGINT, account_id BIGINT, content VARCHAR(240), created_at_ms BIGINT)");
        jdbc.execute("CREATE TABLE xw_business_ownership (business_no VARCHAR(20) PRIMARY KEY, owner_admin_user_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(16))");
        jdbc.update("INSERT INTO customer_orders VALUES ('O1','XW1'),('O2','XW2')");
        jdbc.update("INSERT INTO customer_order_after_sales_records VALUES (1,'O1','DELIVERY','交付或进度','请核对进度',1000),(2,'O2','OTHER','其他问题','跨范围记录',2000)");
        jdbc.update("INSERT INTO xw_departments VALUES (3,'03'),(4,'04')");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XW1',7,3),('XW2',8,4)");
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

    @Test
    void queueFiltersWithinScopeBeforeCountAndPagination() {
        jdbc.update("INSERT INTO customer_order_after_sales_records VALUES (3,'O1','OTHER','其他问题','另一条说明',3000)");
        cases.update("XW1", 1, "RESOLVED", "已处理", 0, actor, null);

        var all = cases.queue(null, null, null, 0, actor);
        assertEquals(2, all.totalCount());
        assertEquals(3, all.items().getFirst().record().recordId());
        assertEquals("XW1", all.items().getFirst().businessNo());
        assertTrue(all.items().stream().noneMatch(item -> "XW2".equals(item.businessNo())));

        var open = cases.queue("OPEN", "OTHER", "另一条", 0, actor);
        assertEquals(1, open.totalCount());
        assertEquals(3, open.items().getFirst().record().recordId());
        assertEquals(0, cases.queue("OPEN", null, "跨范围", 0, actor).totalCount());
        assertEquals(1, cases.queue("RESOLVED", null, null, 0, actor).totalCount());
    }

    @Test
    void queueRejectsInvalidFiltersAndWithoutReadPermission() {
        assertThrows(IllegalArgumentException.class, () -> cases.queue("UNKNOWN", null, null, 0, actor));
        assertThrows(IllegalArgumentException.class, () -> cases.queue(null, "UNKNOWN", null, 0, actor));
        assertThrows(IllegalArgumentException.class, () -> cases.queue(null, null, null, -1, actor));
        when(permissions.has(actor, "order:view")).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> cases.queue(null, null, null, 0, actor));
    }

    @Test
    void queueAppliesDepartmentAllAndDeniedScopesToCountsAndItems() {
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view"), List.of(), "DEPARTMENT", 7L, "03"));
        assertEquals(1, cases.queue(null, null, null, 0, actor).totalCount());
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "LEGACY_ADMIN", true, List.of("order:view"), List.of(), "ALL", 7L, null));
        assertEquals(2, cases.queue(null, null, null, 0, actor).totalCount());
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "DENIED", false, List.of(), List.of(), "ALL", null, null));
        assertEquals(0, cases.queue(null, null, null, 0, actor).totalCount());
    }

    @Test
    void publicReplyAndInternalNoteAreSeparatelyRecordedInHistory() {
        var result = cases.update("XW1", 1, "IN_PROGRESS", "内部核对订单", "正在核对，请稍候",
                0, actor, null);
        assertEquals("IN_PROGRESS", result.status());
        var history = cases.history("XW1", 1, actor);
        assertEquals(1, history.events().size());
        assertEquals("OPEN", history.events().getFirst().fromStatus());
        assertEquals("内部核对订单", history.events().getFirst().handlingNote());
        assertEquals("正在核对，请稍候", history.events().getFirst().customerReply());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> cases.history("XW1", 2, actor)).getStatusCode().value());
        assertThrows(AccessDeniedException.class, () -> cases.history("XW2", 2, actor));
        assertThrows(IllegalArgumentException.class,
                () -> cases.update("XW1", 1, "RESOLVED", "已解决", " ", 1, actor, null));
        assertEquals(1, cases.history("XW1", 1, actor).events().size());
    }

    @Test
    void sameStatusFollowUpRequiresPublicReplyAndKeepsVersionProtection() {
        cases.update("XW1", 1, "IN_PROGRESS", "开始核对", "已经开始核对", 0, actor, null);
        assertThrows(IllegalArgumentException.class,
                () -> cases.update("XW1", 1, "IN_PROGRESS", "继续核对", null, 1, actor, null));

        var followedUp = cases.update("XW1", 1, "IN_PROGRESS", "内部继续核对",
                "已取得新进展", 1, actor, null);
        assertEquals("IN_PROGRESS", followedUp.status());
        assertEquals(2, followedUp.version());
        var history = cases.history("XW1", 1, actor);
        assertEquals(2, history.events().size());
        assertEquals("IN_PROGRESS", history.events().getLast().fromStatus());
        assertEquals("IN_PROGRESS", history.events().getLast().toStatus());
        assertEquals("已取得新进展", history.events().getLast().customerReply());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> cases.update("XW1", 1, "IN_PROGRESS", "过期", "不应写入", 1, actor, null))
                .getStatusCode().value());
        assertEquals(2, cases.history("XW1", 1, actor).events().size());
    }

    @Test
    void scopedQueueSurfacesRecentCustomerFollowupAndHistory() {
        jdbc.update("INSERT INTO customer_order_after_sales_records VALUES (3,'O1','OTHER','其他问题','较新问题',3000)");
        jdbc.update("INSERT INTO customer_order_after_sales_case_followups(record_id,account_id,content,created_at_ms) VALUES (1,11,'原问题的新补充',4000),(2,12,'跨范围补充',5000)");

        var queue = cases.queue(null, null, null, 0, actor);
        assertEquals(2, queue.totalCount());
        assertEquals(1, queue.items().getFirst().record().recordId());
        assertEquals(4000L, queue.items().getFirst().latestCustomerFollowupAtMs());
        assertTrue(queue.items().stream().noneMatch(item -> item.businessNo().equals("XW2")));
        var history = cases.history("XW1", 1, actor);
        assertEquals("原问题的新补充", history.customerFollowups().getFirst().content());
        assertThrows(AccessDeniedException.class, () -> cases.history("XW2", 2, actor));
    }

    @Test
    void pendingFollowupRemainsUntilStaffHandlesTheSeenMessage() {
        jdbc.update("INSERT INTO customer_order_after_sales_case_followups(record_id,account_id,content,created_at_ms) VALUES (1,11,'第一条补充',4000)");
        long firstId = jdbc.queryForObject("SELECT MAX(id) FROM customer_order_after_sales_case_followups WHERE record_id = 1", Long.class);
        assertEquals(1, cases.queue(null, null, "PENDING", null, 0, actor).totalCount());
        assertTrue(cases.queue(null, null, "PENDING", null, 0, actor).items().getFirst().pendingCustomerFollowup());

        jdbc.update("INSERT INTO customer_order_after_sales_case_followups(record_id,account_id,content,created_at_ms) VALUES (1,11,'处理期间的新补充',5000)");
        assertEquals("IN_PROGRESS", cases.update("XW1", 1, "IN_PROGRESS", "已核对第一条",
                "正在处理", firstId, 0, actor, null).status());
        assertEquals(firstId, cases.history("XW1", 1, actor).handledThroughFollowupId());
        assertEquals(1, cases.queue(null, null, "PENDING", null, 0, actor).totalCount());

        long secondId = jdbc.queryForObject("SELECT MAX(id) FROM customer_order_after_sales_case_followups WHERE record_id = 1", Long.class);
        assertThrows(ResponseStatusException.class, () -> cases.update("XW1", 1, "RESOLVED",
                "过期操作", null, secondId, 0, actor, null));
        assertEquals(firstId, cases.history("XW1", 1, actor).handledThroughFollowupId());
        assertThrows(IllegalArgumentException.class, () -> cases.update("XW1", 1, "RESOLVED",
                "伪造编号", null, secondId + 100, 1, actor, null));
        assertEquals("RESOLVED", cases.update("XW1", 1, "RESOLVED", "已核对第二条",
                "已完成", secondId, 1, actor, null).status());
        assertEquals(0, cases.queue(null, null, "PENDING", null, 0, actor).totalCount());
        assertEquals(secondId, cases.history("XW1", 1, actor).handledThroughFollowupId());
    }

    @Test
    void pendingFilterRespectsScopeAndRejectsUnknownValue() {
        jdbc.update("INSERT INTO customer_order_after_sales_case_followups(record_id,account_id,content,created_at_ms) VALUES (2,12,'其他部门补充',5000)");
        assertEquals(0, cases.queue(null, null, "PENDING", null, 0, actor).totalCount());
        assertThrows(IllegalArgumentException.class,
                () -> cases.queue(null, null, "MAYBE", null, 0, actor));
    }

    @Test
    void olderStaffClientDoesNotSilentlyAcknowledgeCustomerFollowup() {
        jdbc.update("INSERT INTO customer_order_after_sales_case_followups(record_id,account_id,content,created_at_ms) VALUES (1,11,'待客服核对',4000)");
        cases.update("XW1", 1, "IN_PROGRESS", "开始核对", "已经开始", 0, actor, null);
        assertEquals(0, cases.history("XW1", 1, actor).handledThroughFollowupId());
        assertEquals(1, cases.queue(null, null, "PENDING", null, 0, actor).totalCount());
    }
}
