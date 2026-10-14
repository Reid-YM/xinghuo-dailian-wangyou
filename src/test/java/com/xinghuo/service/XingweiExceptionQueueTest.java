package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.security.XingweiPermissionGuard;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;

class XingweiExceptionQueueTest {
    @Test
    void exceptionFilterAndDepartmentScopeAreAppliedBeforeTheOrderLimit() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        XingweiPermissionGuard permissions = mock(XingweiPermissionGuard.class);
        Authentication authentication = mock(Authentication.class);
        when(permissions.resolve(authentication)).thenReturn(new XingweiPermissionGuard.AccessSnapshot(
                "SHADOW_RBAC", false, List.of("order:view"), List.of("XW_CUSTOMER_ORDER"),
                "DEPARTMENT", 9L, "03"));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        XingweiBusinessScopeService service = new XingweiBusinessScopeService(jdbc, permissions, mock(AuditService.class));
        service.exceptionOrderSnapshot(authentication);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        String query = sql.getValue();
        assertTrue(query.contains("WHERE d.code = ? AND o2.status = 'EXCEPTION'"));
        assertTrue(query.indexOf("o2.status = 'EXCEPTION'") < query.indexOf("LIMIT 100"));
    }

    @Test
    void afterSalesSearchAndDepartmentScopeAreAppliedBeforePagination() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        XingweiPermissionGuard permissions = mock(XingweiPermissionGuard.class);
        Authentication authentication = mock(Authentication.class);
        when(permissions.resolve(authentication)).thenReturn(new XingweiPermissionGuard.AccessSnapshot(
                "SHADOW_RBAC", false, List.of("order:view"), List.of("XW_CUSTOMER_ORDER"),
                "DEPARTMENT", 9L, "03"));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(120L);

        XingweiBusinessScopeService service = new XingweiBusinessScopeService(jdbc, permissions, mock(AuditService.class));
        Map<String, Object> result = service.afterSalesOrderSnapshot(authentication, " XH100 ", 1);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class), args.capture());
        String query = sql.getValue();
        assertTrue(query.contains("WHERE d.code = ? AND LOCATE('[售后问题 ·', COALESCE(o2.customer_note, '')) > 0"));
        assertTrue(query.contains("LOCATE(?, COALESCE(o2.business_no, o2.order_no)) > 0"));
        assertTrue(query.indexOf("LOCATE('[售后问题 ·'") < query.indexOf("LIMIT ? OFFSET ?"));
        assertArrayEquals(new Object[] {"03", "XH100", "XH100", "XH100", 50, 50}, args.getValue());
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(countSql.capture(), eq(Long.class), any(Object[].class));
        assertTrue(countSql.getValue().contains("WHERE d.code = ? AND LOCATE('[售后问题 ·'"));
        assertEquals(120L, result.get("totalCount"));
        assertEquals(1, result.get("page"));
        assertTrue(Boolean.TRUE.equals(result.get("hasNext")));
    }

    @Test
    void afterSalesRejectsUnboundedPageAndSearch() {
        XingweiBusinessScopeService service = new XingweiBusinessScopeService(
                mock(JdbcTemplate.class), mock(XingweiPermissionGuard.class), mock(AuditService.class));
        Authentication authentication = mock(Authentication.class);
        assertThrows(IllegalArgumentException.class,
                () -> service.afterSalesOrderSnapshot(authentication, "", -1));
        assertThrows(IllegalArgumentException.class,
                () -> service.afterSalesOrderSnapshot(authentication, "x".repeat(81), 0));
    }
}
