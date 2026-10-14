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

import com.xinghuo.account.service.AfterSalesNoteParser;
import com.xinghuo.security.XingweiPermissionGuard;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;

class XingweiExceptionQueueTest {
    @Test
    void afterSalesListReturnsOnlyParsedHistoryInEachScopedOrder() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        XingweiPermissionGuard permissions = mock(XingweiPermissionGuard.class);
        Authentication authentication = mock(Authentication.class);
        ResultSet row = mock(ResultSet.class);
        String note = """
                普通备注

                [售后问题 · 退款核对 · 2026-10-14T10:00:00Z]
                请核对金额

                [用户补充 2026-10-14T11:00:00Z]
                新的普通备注
                """;
        when(row.getString("customer_note")).thenReturn(note);
        when(row.getString("business_no")).thenReturn("XH-1");
        when(permissions.resolve(authentication)).thenReturn(new XingweiPermissionGuard.AccessSnapshot(
                "SHADOW_RBAC", false, List.of("order:view"), List.of("XW_CUSTOMER_ORDER"),
                "DEPARTMENT", 9L, "03"));
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<Map<String, Object>> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(row, 0));
        });

        var result = new XingweiBusinessScopeService(jdbc, permissions, mock(AuditService.class))
                .afterSalesOrderSnapshot(authentication, null, 0);
        @SuppressWarnings("unchecked")
        var items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals(note, items.getFirst().get("customerNote"));
        assertEquals(List.of(new AfterSalesNoteParser.AfterSalesRecord(
                        "退款核对", Instant.parse("2026-10-14T10:00:00Z"), "请核对金额")),
                items.getFirst().get("afterSalesRecords"));
        assertEquals("DEPARTMENT", result.get("dataScope"));
    }

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
        assertThrows(IllegalArgumentException.class,
                () -> service.afterSalesOrderSnapshot(authentication, "", 0, "UNLISTED"));
    }

    @Test
    void afterSalesCategoryUsesWhitelistedMarkerWithinScopeForCountAndPage() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        XingweiPermissionGuard permissions = mock(XingweiPermissionGuard.class);
        Authentication authentication = mock(Authentication.class);
        when(permissions.resolve(authentication)).thenReturn(new XingweiPermissionGuard.AccessSnapshot(
                "SHADOW_RBAC", false, List.of("order:view"), List.of("XW_CUSTOMER_ORDER"),
                "DEPARTMENT", 9L, "03"));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);

        XingweiBusinessScopeService service = new XingweiBusinessScopeService(jdbc, permissions, mock(AuditService.class));
        Map<String, Object> result = service.afterSalesOrderSnapshot(authentication, " XH100 ", 0, "REFUND_REVIEW");

        ArgumentCaptor<String> pageSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> pageArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(pageSql.capture(), any(RowMapper.class), pageArgs.capture());
        assertTrue(pageSql.getValue().contains("WHERE d.code = ? AND LOCATE('[售后问题 ·'"));
        assertTrue(pageSql.getValue().contains("AND LOCATE(?, COALESCE(o2.customer_note, '')) > 0"));
        assertArrayEquals(new Object[] {"03", "[售后问题 · 退款核对 · ", "XH100", "XH100", "XH100", 50, 0},
                pageArgs.getValue());

        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> countArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForObject(countSql.capture(), eq(Long.class), countArgs.capture());
        assertTrue(countSql.getValue().contains("AND LOCATE(?, COALESCE(o2.customer_note, '')) > 0"));
        assertArrayEquals(new Object[] {"03", "[售后问题 · 退款核对 · ", "XH100", "XH100", "XH100"},
                countArgs.getValue());
        assertEquals("REFUND_REVIEW", result.get("category"));
        assertEquals(1L, result.get("totalCount"));
    }
}
