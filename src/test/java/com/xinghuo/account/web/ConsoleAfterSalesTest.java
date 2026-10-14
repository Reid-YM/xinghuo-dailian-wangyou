package com.xinghuo.account.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void fullCustomerNoteReturnsConflictWithoutLosingExistingText() {
        jdbc.update("UPDATE customer_orders SET customer_note=? WHERE id='order-1'", "x".repeat(490));
        assertEquals("AFTER_SALES_NOTE_FULL", assertThrows(AccountException.class, () -> controller.submitAfterSales(
                "XH-1", new ConsoleApiController.AfterSalesRequest("OTHER", "继续处理"), request)).getCode());
        assertEquals("x".repeat(490), jdbc.queryForObject("SELECT customer_note FROM customer_orders WHERE id='order-1'", String.class));
        verifyNoInteractions(audit);
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
}
