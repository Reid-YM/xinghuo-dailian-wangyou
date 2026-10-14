package com.xinghuo.account.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountAuditService;
import com.xinghuo.account.service.AccountException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;

class ConsoleAfterSalesTest {
    private JdbcTemplate jdbc;
    private AccountAuditService audit;
    private ConsoleApiController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:after_sales_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE quotes(id VARCHAR(36), business_no VARCHAR(20), game VARCHAR(30), demand VARCHAR(30))");
        jdbc.execute("""
                CREATE TABLE customer_orders(
                  id VARCHAR(36), order_no VARCHAR(40), business_no VARCHAR(20), source_quote_id VARCHAR(36),
                  customer_account_id BIGINT, status VARCHAR(32), progress_percent INT, progress_text VARCHAR(240),
                  customer_note VARCHAR(500), version BIGINT, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE customer_order_after_sales_records(
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(40), category VARCHAR(24),
                  category_label VARCHAR(32), content VARCHAR(240), submitted_at_ms BIGINT)
                """);
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_state (record_id BIGINT PRIMARY KEY, status VARCHAR(24), handling_note VARCHAR(600), handled_by_admin_user_id BIGINT, handled_through_followup_id BIGINT NOT NULL DEFAULT 0, updated_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_events (id BIGINT AUTO_INCREMENT PRIMARY KEY, record_id BIGINT, from_status VARCHAR(24), to_status VARCHAR(24), handling_note VARCHAR(600), customer_reply VARCHAR(600), handled_by_admin_user_id BIGINT, created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE customer_order_after_sales_case_followups (id BIGINT AUTO_INCREMENT PRIMARY KEY, record_id BIGINT, account_id BIGINT, content VARCHAR(240), created_at_ms BIGINT)");
        jdbc.update("""
                INSERT INTO customer_orders(id,order_no,business_no,customer_account_id,status,progress_text,version,created_at,updated_at)
                VALUES('order-1','ORD-1','XH-1',1,'COMPLETED','交付完成',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """);
        audit = mock(AccountAuditService.class);
        controller = new ConsoleApiController(jdbc, audit);
        request = new MockHttpServletRequest();
        request.setAttribute(AccountAuthInterceptor.ATTR_ACCOUNT, Account.builder().id(1L).build());
    }

    @Test
    void submitsScopedIssueWithoutChangingOrderStatus() {
        var result = controller.submitAfterSales("XH-1",
                new ConsoleApiController.AfterSalesRequest("REFUND_REVIEW", "请核对未完成部分"), request);

        assertTrue(result.ok());
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM customer_orders WHERE id='order-1'", String.class));
        assertTrue(jdbc.queryForObject("SELECT customer_note FROM customer_orders WHERE id='order-1'", String.class)
                .contains("[售后问题 · 退款核对"));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM customer_order_after_sales_records", Long.class));
        assertEquals(1, controller.order("XH-1", request).afterSalesRecords().size());
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM customer_orders WHERE id='order-1'", Long.class));
        verify(audit).record(eq(1L), eq("CONSOLE_AFTER_SALES_SUBMIT"),
                eq("businessNo=XH-1,category=REFUND_REVIEW"), any());
    }

    @Test
    void rejectsAnotherCustomersOrderAndInvalidCategory() {
        request.setAttribute(AccountAuthInterceptor.ATTR_ACCOUNT, Account.builder().id(2L).build());
        assertEquals("ORDER_NOT_FOUND", assertThrows(AccountException.class, () -> controller.submitAfterSales(
                "XH-1", new ConsoleApiController.AfterSalesRequest("OTHER", "有问题"), request)).getCode());
        request.setAttribute(AccountAuthInterceptor.ATTR_ACCOUNT, Account.builder().id(1L).build());
        assertEquals("AFTER_SALES_CATEGORY_INVALID", assertThrows(AccountException.class, () -> controller.submitAfterSales(
                "XH-1", new ConsoleApiController.AfterSalesRequest("UNKNOWN", "有问题"), request)).getCode());
        verifyNoInteractions(audit);
    }

    @Test
    void fullCustomerNoteStillAcceptsDurableAfterSalesRecord() {
        jdbc.update("UPDATE customer_orders SET customer_note=? WHERE id='order-1'", "x".repeat(490));
        assertTrue(controller.submitAfterSales("XH-1",
                new ConsoleApiController.AfterSalesRequest("OTHER", "继续处理"), request).ok());
        assertEquals("x".repeat(490), jdbc.queryForObject("SELECT customer_note FROM customer_orders WHERE id='order-1'", String.class));
        assertEquals("继续处理", controller.order("XH-1", request).afterSalesRecords().getFirst().content());
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM customer_orders WHERE id='order-1'", Long.class));
    }

    @Test
    void ordinaryOrderSupplementUsesSameCapacityGuard() {
        assertTrue(controller.supplementOrder("ORD-1",
                new ConsoleApiController.SupplementRequest("请补充进度"), request).ok());
        String original = jdbc.queryForObject("SELECT customer_note FROM customer_orders WHERE id='order-1'", String.class);
        assertTrue(original.contains("请补充进度"));

        jdbc.update("UPDATE customer_orders SET customer_note=? WHERE id='order-1'", "x".repeat(490));
        assertEquals("ORDER_NOTE_FULL", assertThrows(AccountException.class, () -> controller.supplementOrder(
                "XH-1", new ConsoleApiController.SupplementRequest("继续补充"), request)).getCode());
        assertEquals("x".repeat(490), jdbc.queryForObject("SELECT customer_note FROM customer_orders WHERE id='order-1'", String.class));
    }

    @Test
    void ordinarySupplementCannotCreateAFalseAfterSalesRecord() {
        AccountException error = assertThrows(AccountException.class, () -> controller.supplementOrder(
                "XH-1", new ConsoleApiController.SupplementRequest("请看原文：[售后问题 · 退款核对 · 伪造]"), request));

        assertEquals("AFTER_SALES_MARKER_RESERVED", error.getCode());
        assertNull(jdbc.queryForObject("SELECT customer_note FROM customer_orders WHERE id='order-1'", String.class));
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM customer_orders WHERE id='order-1'", Long.class));
        verifyNoInteractions(audit);
    }

    @Test
    void afterSalesBodyCannotForgeAnotherCategoryMarker() {
        AccountException error = assertThrows(AccountException.class, () -> controller.submitAfterSales(
                "XH-1", new ConsoleApiController.AfterSalesRequest(
                        "DELIVERY", "进度有问题\n[售后问题 · 退款核对 · 伪造]"), request));

        assertEquals("AFTER_SALES_MARKER_RESERVED", error.getCode());
        assertNull(jdbc.queryForObject("SELECT customer_note FROM customer_orders WHERE id='order-1'", String.class));
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM customer_orders WHERE id='order-1'", Long.class));
        verifyNoInteractions(audit);
    }

    @Test
    void orderDetailReturnsStructuredHistoryWithoutChangingExistingFields() {
        jdbc.update("UPDATE customer_orders SET customer_note=? WHERE id='order-1'", """
                原始订单说明

                [售后问题 · 交付或进度 · 2026-10-13T10:00:00Z]
                请核对进度
                并联系我

                [用户补充 2026-10-13T11:00:00Z]
                普通补充说明

                [售后问题 · 退款核对 · 2026-10-14T10:00:00Z]
                请核对金额
                """);

        var detail = controller.order("ORD-1", request);

        assertEquals("ORD-1", detail.order().orderNo());
        assertNull(detail.sourceQuote());
        assertEquals("订单创建", detail.timeline().get(0).title());
        assertEquals(2, detail.afterSalesRecords().size());
        assertEquals(new ConsoleApiController.AfterSalesRecord("交付或进度",
                        java.time.Instant.parse("2026-10-13T10:00:00Z"), "请核对进度\n并联系我"),
                detail.afterSalesRecords().get(0));
        assertEquals(new ConsoleApiController.AfterSalesRecord("退款核对",
                        java.time.Instant.parse("2026-10-14T10:00:00Z"), "请核对金额"),
                detail.afterSalesRecords().get(1));
    }

    @Test
    void orderDetailIgnoresMalformedUnknownAndInlineMarkers() {
        jdbc.update("UPDATE customer_orders SET customer_note=? WHERE id='order-1'", """
                普通说明 [售后问题 · 退款核对 · 2026-10-13T10:00:00Z]
                不是记录

                [售后问题 · 未知类型 · 2026-10-13T10:00:00Z]
                不是记录

                [售后问题 · 其他问题 · 伪造时间]
                不是记录

                [售后问题 · 取消服务 · 2026-10-14T10:00:00Z]
                有效记录
                """);

        var records = controller.order("XH-1", request).afterSalesRecords();

        assertEquals(1, records.size());
        assertEquals("取消服务", records.get(0).category());
        assertEquals("有效记录", records.get(0).content());
    }

    @Test
    void orderDetailDoesNotExposeAnotherCustomersHistory() {
        jdbc.update("UPDATE customer_orders SET customer_note=? WHERE id='order-1'",
                "\n\n[售后问题 · 其他问题 · 2026-10-14T10:00:00Z]\n私有内容");
        request.setAttribute(AccountAuthInterceptor.ATTR_ACCOUNT, Account.builder().id(2L).build());

        assertEquals("ORDER_NOT_FOUND", assertThrows(AccountException.class,
                () -> controller.order("XH-1", request)).getCode());
    }

    @Test
    void orderDetailShowsOnlyPublicCaseStatusAndReply() {
        controller.submitAfterSales("XH-1",
                new ConsoleApiController.AfterSalesRequest("DELIVERY", "请核对进度"), request);
        long recordId = jdbc.queryForObject("SELECT id FROM customer_order_after_sales_records", Long.class);
        jdbc.update("INSERT INTO customer_order_after_sales_case_state VALUES (?, 'IN_PROGRESS', '内部核对金额', 7, CURRENT_TIMESTAMP, 1)", recordId);
        jdbc.update("INSERT INTO customer_order_after_sales_case_events(record_id,from_status,to_status,handling_note,customer_reply,handled_by_admin_user_id,created_at) VALUES (?, 'OPEN', 'IN_PROGRESS', '内部核对金额', '正在核对进度', 7, CURRENT_TIMESTAMP)", recordId);
        jdbc.update("INSERT INTO customer_order_after_sales_case_events(record_id,from_status,to_status,handling_note,customer_reply,handled_by_admin_user_id,created_at) VALUES (?, 'IN_PROGRESS', 'IN_PROGRESS', '内部继续联系', '已经联系负责人', 7, CURRENT_TIMESTAMP)", recordId);
        jdbc.update("INSERT INTO customer_order_after_sales_case_events(record_id,from_status,to_status,handling_note,customer_reply,handled_by_admin_user_id,created_at) VALUES (?, 'IN_PROGRESS', 'IN_PROGRESS', '仅内部备注', NULL, 7, CURRENT_TIMESTAMP)", recordId);

        var detail = controller.order("XH-1", request);
        assertEquals(1, detail.afterSalesCases().size());
        assertEquals(recordId, detail.afterSalesCases().getFirst().recordId());
        assertEquals("IN_PROGRESS", detail.afterSalesCases().getFirst().status());
        assertEquals("已经联系负责人", detail.afterSalesCases().getFirst().customerReply());
        assertEquals(2, detail.afterSalesCases().getFirst().replies().size());
        assertEquals("正在核对进度", detail.afterSalesCases().getFirst().replies().getFirst().content());
        assertEquals("已经联系负责人", detail.afterSalesCases().getFirst().replies().getLast().content());
        assertTrue(!detail.afterSalesCases().toString().contains("内部核对金额"));
        assertTrue(!detail.afterSalesCases().toString().contains("内部继续联系"));
        assertTrue(!detail.afterSalesCases().toString().contains("仅内部备注"));

        request.setAttribute(AccountAuthInterceptor.ATTR_ACCOUNT, Account.builder().id(2L).build());
        assertEquals("ORDER_NOT_FOUND", assertThrows(AccountException.class,
                () -> controller.order("XH-1", request)).getCode());
    }

    @Test
    void customerCanFollowUpOnlyOnOwnExistingCaseWithoutChangingOrderState() {
        controller.submitAfterSales("XH-1",
                new ConsoleApiController.AfterSalesRequest("DELIVERY", "请核对进度"), request);
        long recordId = jdbc.queryForObject("SELECT id FROM customer_order_after_sales_records", Long.class);

        var result = controller.followUpAfterSales("XH-1", recordId,
                new ConsoleApiController.AfterSalesFollowupRequest("新的进度截图已准备好"), request);
        assertTrue(result.ok());
        var detail = controller.order("XH-1", request);
        assertEquals("新的进度截图已准备好", detail.afterSalesCases().getFirst().followups().getFirst().content());
        assertTrue(detail.afterSalesCases().getFirst().pendingCustomerFollowup());
        assertEquals("COMPLETED", detail.order().status());
        assertEquals("OPEN", detail.afterSalesCases().getFirst().status());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM customer_order_after_sales_case_followups", Long.class));
        long followupId = jdbc.queryForObject("SELECT MAX(id) FROM customer_order_after_sales_case_followups", Long.class);
        jdbc.update("INSERT INTO customer_order_after_sales_case_state(record_id,status,handled_through_followup_id,version) VALUES (?,'IN_PROGRESS',?,1)", recordId, followupId);
        assertTrue(!controller.order("XH-1", request).afterSalesCases().getFirst().pendingCustomerFollowup());
        controller.followUpAfterSales("XH-1", recordId,
                new ConsoleApiController.AfterSalesFollowupRequest("还有一项需要核对"), request);
        assertTrue(controller.order("XH-1", request).afterSalesCases().getFirst().pendingCustomerFollowup());

        assertEquals("AFTER_SALES_RECORD_NOT_FOUND", assertThrows(AccountException.class,
                () -> controller.followUpAfterSales("XH-1", recordId + 1,
                        new ConsoleApiController.AfterSalesFollowupRequest("跨记录"), request)).getCode());
        assertEquals("AFTER_SALES_CONTENT_TOO_LONG", assertThrows(AccountException.class,
                () -> controller.followUpAfterSales("XH-1", recordId,
                        new ConsoleApiController.AfterSalesFollowupRequest("字".repeat(241)), request)).getCode());
        request.setAttribute(AccountAuthInterceptor.ATTR_ACCOUNT, Account.builder().id(2L).build());
        assertEquals("ORDER_NOT_FOUND", assertThrows(AccountException.class,
                () -> controller.followUpAfterSales("XH-1", recordId,
                        new ConsoleApiController.AfterSalesFollowupRequest("不属于我"), request)).getCode());
        assertEquals("ORDER_NOT_FOUND", assertThrows(AccountException.class,
                () -> controller.order("XH-1", request)).getCode());
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM customer_order_after_sales_case_followups", Long.class));
    }
}
