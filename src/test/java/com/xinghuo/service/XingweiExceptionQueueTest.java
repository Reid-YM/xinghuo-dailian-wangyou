package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.security.XingweiPermissionGuard;
import java.util.List;
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
    void afterSalesRecordFilterAndDepartmentScopeAreAppliedBeforeTheOrderLimit() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        XingweiPermissionGuard permissions = mock(XingweiPermissionGuard.class);
        Authentication authentication = mock(Authentication.class);
        when(permissions.resolve(authentication)).thenReturn(new XingweiPermissionGuard.AccessSnapshot(
                "SHADOW_RBAC", false, List.of("order:view"), List.of("XW_CUSTOMER_ORDER"),
                "DEPARTMENT", 9L, "03"));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        XingweiBusinessScopeService service = new XingweiBusinessScopeService(jdbc, permissions, mock(AuditService.class));
        service.afterSalesOrderSnapshot(authentication);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(RowMapper.class), any(Object[].class));
        String query = sql.getValue();
        assertTrue(query.contains("WHERE d.code = ? AND LOCATE('[售后问题 ·', COALESCE(o2.customer_note, '')) > 0"));
        assertTrue(query.indexOf("LOCATE('[售后问题 ·'") < query.indexOf("LIMIT 100"));
    }
}
