package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class XingweiAfterSalesAttentionQueueServiceTest {
    private JdbcTemplate jdbc;
    private XingweiPermissionGuard permissions;
    private XingweiAfterSalesAttentionQueueService queue;
    private Authentication actor;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:xw_attention_queue_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE customer_orders (order_no VARCHAR(40) PRIMARY KEY, business_no VARCHAR(20))");
        jdbc.execute("CREATE TABLE customer_order_after_sales_records (id BIGINT PRIMARY KEY, order_no VARCHAR(40), category VARCHAR(24), category_label VARCHAR(64), content VARCHAR(255), submitted_at_ms BIGINT)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_state (record_id BIGINT PRIMARY KEY, status VARCHAR(24), handling_note VARCHAR(255), handled_by_admin_user_id BIGINT, handled_through_followup_id BIGINT DEFAULT 0, updated_at TIMESTAMP, version BIGINT DEFAULT 0)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_followups (id BIGINT PRIMARY KEY, record_id BIGINT, content VARCHAR(255), created_at_ms BIGINT)");
        jdbc.execute("CREATE TABLE xw_business_ownership (business_no VARCHAR(20) PRIMARY KEY, owner_admin_user_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(16))");

        long now = System.currentTimeMillis();
        long h1 = now - Duration.ofHours(1).toMillis();
        long h30 = now - Duration.ofHours(30).toMillis();
        long h80 = now - Duration.ofHours(80).toMillis();
        jdbc.update("INSERT INTO customer_orders VALUES ('O1','XW1'),('O2','XW2')");
        jdbc.update("INSERT INTO xw_departments VALUES (3,'03'),(4,'04')");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XW1',7,3),('XW2',8,4)");
        jdbc.update("INSERT INTO customer_order_after_sales_records VALUES (1,'O1','DELIVERY','交付或进度','A',?), (2,'O1','OTHER','其他问题','B',?), (3,'O2','REFUND_REVIEW','退款核对','C',?), (4,'O2','CANCELLATION','取消服务','D',?)",
                h1, h80, h80, h1);
        jdbc.update("INSERT INTO customer_order_after_sales_case_state(record_id,status,handling_note,handled_by_admin_user_id,handled_through_followup_id,updated_at,version) VALUES (2,'IN_PROGRESS','处理中',7,0,NOW(),1),(3,'RESOLVED','已解决',8,0,NOW(),1),(4,'IN_PROGRESS','处理中',8,0,NOW(),1)");
        jdbc.update("INSERT INTO customer_order_after_sales_case_followups VALUES (11,2,'新补充',?),(12,3,'旧补充',?)", h30, h80);

        permissions = mock(XingweiPermissionGuard.class);
        actor = new UsernamePasswordAuthenticationToken("staff", "", List.of());
        when(permissions.has(actor, "order:view")).thenReturn(true);
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view"), List.of(), "SELF", 7L, "03"));
        queue = new XingweiAfterSalesAttentionQueueService(jdbc, permissions);
    }

    @Test
    void queueUsesAttentionSemanticsAndMutuallyExclusiveBuckets() {
        var within = queue.queue("WITHIN_24H", null, null, null, null, 0, actor);
        assertEquals(1, within.totalCount());
        assertEquals(1L, within.items().getFirst().record().recordId());
        assertEquals("WITHIN_24H", within.items().getFirst().ageBucket());
        assertTrue(within.items().getFirst().waitingMs() >= Duration.ofMinutes(59).toMillis());
        assertTrue(within.items().getFirst().waitingMs() < Duration.ofHours(24).toMillis());

        var middle = queue.queue("FROM_24_TO_72H", null, null, null, null, 0, actor);
        assertEquals(1, middle.totalCount());
        assertEquals(2L, middle.items().getFirst().record().recordId());
        assertEquals(true, middle.items().getFirst().pendingCustomerFollowup());
        assertEquals("FROM_24_TO_72H", middle.items().getFirst().ageBucket());
        assertEquals(middle.items().getFirst().latestCustomerFollowupAtMs().longValue(),
                middle.items().getFirst().attentionAtMs());
        assertTrue(middle.items().getFirst().waitingMs() >= Duration.ofHours(30).minusMinutes(1).toMillis());

        var allAttention = queue.queue("ATTENTION", null, null, null, null, 0, actor);
        assertEquals(2, allAttention.totalCount());
    }

    @Test
    void queueKeepsDepartmentAllAndDeniedScopes() {
        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "RBAC", false, List.of("order:view"), List.of(), "DEPARTMENT", 8L, "04"));
        var department = queue.queue("OVER_72H", null, null, null, null, 0, actor);
        assertEquals(1, department.totalCount());
        assertEquals(3L, department.items().getFirst().record().recordId());
        assertEquals("OVER_72H", department.items().getFirst().ageBucket());
        assertTrue(department.items().getFirst().waitingMs() >= Duration.ofHours(80).minusMinutes(1).toMillis());

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "LEGACY_ADMIN", true, List.of("order:view"), List.of(), "ALL", 7L, null));
        assertEquals(3, queue.queue("ATTENTION", null, null, null, null, 0, actor).totalCount());

        when(permissions.resolve(actor)).thenReturn(new AccessSnapshot(
                "DENIED", false, List.of(), List.of(), "ALL", null, null));
        assertEquals(0, queue.queue("ATTENTION", null, null, null, null, 0, actor).totalCount());
    }

    @Test
    void queuePreservesExistingFiltersAndRejectsInvalidAge() {
        var filtered = queue.queue("ATTENTION", "IN_PROGRESS", "OTHER", "PENDING", "XW1", 0, actor);
        assertEquals(1, filtered.totalCount());
        assertEquals(2L, filtered.items().getFirst().record().recordId());
        assertEquals("IN_PROGRESS", filtered.status());
        assertEquals("OTHER", filtered.category());
        assertEquals("PENDING", filtered.followup());
        assertEquals("XW1", filtered.query());
        assertThrows(IllegalArgumentException.class,
                () -> queue.queue("UNKNOWN", null, null, null, null, 0, actor));
    }

    @Test
    void queueSupportsOldestFirstWithoutChangingTheDefaultOrder() {
        var newest = queue.queue("ATTENTION", null, null, null, null, "NEWEST_FIRST", 0, actor);
        assertEquals(List.of(1L, 2L), newest.items().stream().map(item -> item.record().recordId()).toList());

        var oldest = queue.queue("ATTENTION", null, null, null, null, "OLDEST_FIRST", 0, actor);
        assertEquals(List.of(2L, 1L), oldest.items().stream().map(item -> item.record().recordId()).toList());

        assertThrows(IllegalArgumentException.class,
                () -> queue.queue("ATTENTION", null, null, null, null, "UNKNOWN", 0, actor));
    }

    @Test
    void queueRequiresOrderViewPermission() {
        when(permissions.has(actor, "order:view")).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> queue.queue("ATTENTION", null, null, null, null, 0, actor));
    }
}
