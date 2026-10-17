package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.xinghuo.account.service.AutomaticOrderAccountLinkService;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.payment.WeChatNativePaymentService;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class XingweiPaymentWorkspaceServiceTest {
    JdbcTemplate jdbc;
    XingweiPermissionGuard guard;
    AuditService audits;
    ObjectProvider<WeChatNativePaymentService> provider;
    WeChatNativePaymentService nativePay;
    AutomaticOrderAccountLinkService accountLinks;
    XingweiPaymentWorkspaceService work;
    Authentication all, dept, self, readOnly;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        var ds = new DriverManagerDataSource(
                "jdbc:h2:mem:paymentworkspace" + System.nanoTime() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE xw_departments(id BIGINT PRIMARY KEY,code VARCHAR(16),name VARCHAR(80))");
        jdbc.update("INSERT INTO xw_departments VALUES(3,'03','本中心'),(4,'04','其他中心')");
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY,username VARCHAR(80))");
        jdbc.execute("CREATE TABLE xw_admin_profiles(admin_user_id BIGINT PRIMARY KEY,display_name VARCHAR(80))");
        jdbc.execute("CREATE TABLE xw_business_ownership(business_no VARCHAR(64) PRIMARY KEY,department_id BIGINT,owner_admin_user_id BIGINT)");
        jdbc.execute("CREATE TABLE customer_orders(order_no VARCHAR(64) PRIMARY KEY,business_no VARCHAR(64),status VARCHAR(24),customer_account_id BIGINT,updated_at TIMESTAMP(6))");
        jdbc.execute("CREATE TABLE payment_order(id BIGINT PRIMARY KEY,payment_no VARCHAR(64),business_no VARCHAR(64),subject VARCHAR(127),amount DECIMAL(10,2),currency VARCHAR(8),status VARCHAR(24),provider VARCHAR(24),created_at TIMESTAMP,paid_at TIMESTAMP,updated_at TIMESTAMP(6),native_code_url VARCHAR(500))");
        for (int i = 1; i <= 3; i++) {
            jdbc.update("INSERT INTO xw_business_ownership VALUES(?,?,?)", "B-" + i, i == 3 ? 4 : 3, i);
            jdbc.update("INSERT INTO customer_orders VALUES(?,?,'NEW',100,CURRENT_TIMESTAMP)", "O-" + i, "B-" + i);
            jdbc.update("INSERT INTO payment_order VALUES(?,?,?,'真实服务',88,'CNY','CREATED','WECHAT',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,'DO-NOT-EXPOSE-QR')", i, "PAY-" + i, "B-" + i);
        }
        guard = mock(XingweiPermissionGuard.class);
        audits = mock(AuditService.class);
        provider = mock(ObjectProvider.class);
        nativePay = mock(WeChatNativePaymentService.class);
        accountLinks = mock(AutomaticOrderAccountLinkService.class);
        when(provider.getIfAvailable()).thenReturn(nativePay);
        all = actor("all", "ALL", true);
        dept = actor("dept", "DEPARTMENT", true);
        self = actor("self", "SELF", true);
        readOnly = actor("readonly", "ALL", false);
        var target = new XingweiPaymentWorkspaceService(
                jdbc,
                guard,
                new XingweiBusinessScopeService(jdbc, guard, audits),
                provider,
                audits,
                accountLinks);
        var tx = new TransactionInterceptor();
        tx.setTransactionManager(new DataSourceTransactionManager(ds));
        tx.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory = new ProxyFactory(target);
        factory.addAdvice(tx);
        work = (XingweiPaymentWorkspaceService) factory.getProxy();
    }

    Authentication actor(String name, String scope, boolean write) {
        Authentication actor = new UsernamePasswordAuthenticationToken(name, "", List.of());
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC", false, List.of(), List.of(), scope, 1L, "03"));
        when(guard.has(actor, "payment:view")).thenReturn(true);
        when(guard.has(actor, "payment:manage")).thenReturn(write);
        return actor;
    }

    @Test
    void paymentScopePrecedesCountAndPaginationAndNeverExposesNativeUrls() {
        assertEquals(3L, work.snapshot(all, "", 0).get("total"));
        assertEquals(2L, work.snapshot(dept, "", 0).get("total"));
        assertEquals(1L, work.snapshot(self, "", 0).get("total"));
        assertFalse(work.snapshot(all, "", 0).toString().contains("DO-NOT-EXPOSE-QR"));
        assertThrows(AccessDeniedException.class, () -> work.order("B-3", self));
        assertEquals(0L, work.snapshot(all, "%", 0).get("total"));
    }

    @Test
    void orderLookupAutomaticallyRepairsMissingCustomerOwnershipAfterScopeCheck() {
        jdbc.update("UPDATE customer_orders SET customer_account_id=NULL WHERE order_no='O-1'");
        doAnswer(invocation -> {
            jdbc.update("UPDATE customer_orders SET customer_account_id=100,updated_at=CURRENT_TIMESTAMP WHERE order_no='O-1'");
            return new AutomaticOrderAccountLinkService.LinkOutcome(true, 100L, "VERIFIED_PRIMARY_CONTACT");
        }).when(accountLinks).reconcile("B-1");

        Map<String, Object> row = work.order("B-1", self);

        assertEquals(true, row.get("customerBound"));
        assertEquals(true, row.get("paymentEligible"));
        assertEquals(true, row.get("customerAutoLinked"));
        verify(accountLinks).reconcile("B-1");
        verify(audits).log(eq("XW_ORDER_ACCOUNT_AUTO_LINK"), eq("ORDER"), eq("B-1"), eq("self"), any(), any(), any(), isNull());
    }

    @Test
    void prepareUsesServerOrderAndExplicitAdminAmountAfterRevisionAndScope() {
        var before = work.order("B-1", self);
        var amount = new BigDecimal("95.50");
        when(nativePay.prepare("O-1", amount, "客服确认服务")).thenReturn(
                new WeChatNativePaymentService.Payment(
                        "PAY-NEW", "B-1", "客服确认服务", amount, "CREATED", Instant.now(), "DO-NOT-EXPOSE-QR"));
        var result = work.prepare(
                "B-1",
                new XingweiPaymentWorkspaceService.PrepareInput(amount, "客服确认服务", "核对最终金额"),
                String.valueOf(before.get("revision")),
                self,
                null);
        assertEquals(amount, result.get("amount"));
        assertFalse(result.toString().contains("DO-NOT-EXPOSE-QR"));
        verify(nativePay).prepare("O-1", amount, "客服确认服务");
        assertThrows(OptimisticLockingFailureException.class, () -> work.prepare(
                "B-1",
                new XingweiPaymentWorkspaceService.PrepareInput(amount, "服务", "说明"),
                "old", self, null));
        assertThrows(AccessDeniedException.class, () -> work.prepare(
                "B-3",
                new XingweiPaymentWorkspaceService.PrepareInput(amount, "服务", "说明"),
                "old", self, null));
        verifyNoMoreInteractions(nativePay);
    }

    @Test
    @SuppressWarnings("unchecked")
    void closeRequiresScopeVersionPermissionAndAuditableRequestBeforeProvider() {
        var items = (List<Map<String, Object>>) work.snapshot(self, "", 0).get("items");
        String revision = String.valueOf(items.getFirst().get("revision"));
        assertThrows(AccessDeniedException.class, () -> work.close("PAY-3", "old", self, null));
        assertThrows(OptimisticLockingFailureException.class, () -> work.close("PAY-1", "old", self, null));
        assertThrows(AccessDeniedException.class, () -> work.close("PAY-1", revision, readOnly, null));
        verifyNoInteractions(nativePay);
        doThrow(new IllegalStateException("audit unavailable"))
                .when(audits).log(any(), any(), any(), any(), any(), any(), any(), any());
        assertThrows(IllegalStateException.class, () -> work.close("PAY-1", revision, self, null));
        verifyNoInteractions(nativePay);
        reset(audits);
        when(nativePay.close("PAY-1")).thenReturn(false);
        assertEquals("CAPTURED", work.close("PAY-1", revision, self, null).get("status"));
        var ordered = inOrder(audits, nativePay);
        ordered.verify(audits).log(eq("XW_PAYMENT_CLOSE_REQUEST"), any(), any(), any(), any(), any(), any(), any());
        ordered.verify(nativePay).close("PAY-1");
    }

    @Test
    void disabledSwitchRemainsReadOnlyAndDoesNotCallNativeProvider() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertEquals(false, work.snapshot(all, "", 0).get("enabled"));
        var row = work.order("B-1", all);
        assertThrows(BadRequestException.class, () -> work.prepare(
                "B-1",
                new XingweiPaymentWorkspaceService.PrepareInput(new BigDecimal("88.00"), "服务", "核对"),
                String.valueOf(row.get("revision")),
                all,
                null));
        verifyNoInteractions(nativePay);
    }
}
