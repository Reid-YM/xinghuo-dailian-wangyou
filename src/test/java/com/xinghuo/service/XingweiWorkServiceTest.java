package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.security.XingweiPermissionGuard;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class XingweiWorkServiceTest {
    private JdbcTemplate jdbc;
    private XingweiWorkService work;
    private OrderService orders;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:xw_work_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE admin_users (id BIGINT PRIMARY KEY, username VARCHAR(64) UNIQUE, enabled BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(16), name VARCHAR(80), enabled BOOLEAN, sort_order INT)");
        jdbc.execute("CREATE TABLE xw_positions (id BIGINT PRIMARY KEY, code VARCHAR(32), name VARCHAR(80), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE xw_admin_profiles (admin_user_id BIGINT PRIMARY KEY, display_name VARCHAR(80), employee_no VARCHAR(32), department_id BIGINT, position_id BIGINT, status VARCHAR(24))");
        jdbc.execute("CREATE TABLE xw_rbac_roles (id BIGINT PRIMARY KEY, code VARCHAR(48), data_scope VARCHAR(32), enabled BOOLEAN NOT NULL, sort_order INT NOT NULL)");
        jdbc.execute("CREATE TABLE xw_permissions (id BIGINT PRIMARY KEY, code VARCHAR(64))");
        jdbc.execute("CREATE TABLE xw_admin_roles (admin_user_id BIGINT, role_id BIGINT, is_primary BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE xw_role_permissions (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE xw_business_ownership (business_no VARCHAR(20) PRIMARY KEY, owner_admin_user_id BIGINT, department_id BIGINT NOT NULL, created_by VARCHAR(64), updated_by VARCHAR(64), created_at TIMESTAMP, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE quotes (id UUID PRIMARY KEY, business_no VARCHAR(20) UNIQUE, game VARCHAR(100), demand VARCHAR(200), expected_time VARCHAR(100))");
        jdbc.execute("CREATE TABLE customer_orders (order_no VARCHAR(40) PRIMARY KEY, business_no VARCHAR(20), source_quote_id UUID, status VARCHAR(32), progress_percent INT, progress_text VARCHAR(240), progress_image_asset_id VARCHAR(64), customer_note VARCHAR(500), internal_note VARCHAR(1200), updated_at TIMESTAMP, version BIGINT)");

        jdbc.update("INSERT INTO admin_users VALUES (1,'admin',TRUE),(2,'dept',TRUE),(3,'self',TRUE),(4,'peer03',TRUE),(5,'peer04',TRUE)");
        jdbc.update("INSERT INTO xw_departments VALUES (1,'01','01 管理层',TRUE,1),(3,'03','03 客服与订单中心',TRUE,3),(4,'04','04 游戏业务中心',TRUE,4)");
        jdbc.update("INSERT INTO xw_admin_profiles VALUES (1,'管理员','A01',1,NULL,'ACTIVE'),(2,'调度员','D02',3,NULL,'ACTIVE'),(3,'本人','S03',3,NULL,'ACTIVE'),(4,'同中心员工','P04',3,NULL,'ACTIVE'),(5,'跨中心员工','P05',4,NULL,'ACTIVE')");
        jdbc.update("INSERT INTO xw_rbac_roles VALUES (1,'XW_DEPT','DEPARTMENT',TRUE,1),(2,'XW_SELF','SELF',TRUE,2)");
        jdbc.update("INSERT INTO xw_permissions VALUES (1,'order:view'),(2,'order:manage'),(3,'order:assign')");
        jdbc.update("INSERT INTO xw_admin_roles VALUES (2,1,TRUE),(3,2,TRUE)");
        jdbc.update("INSERT INTO xw_role_permissions VALUES (1,1),(1,2),(1,3),(2,1),(2,2)");

        addOrder("11111111-1111-1111-1111-111111111111", "XWMINE03", "MW4", "本人任务", "今晚", "IN_PROGRESS", 20, "处理中");
        addOrder("22222222-2222-2222-2222-222222222222", "XWQUEUE03", "MW4", "中心队列", "今天", "WAITING_CONFIRMATION", null, "待领取");
        addOrder("33333333-3333-3333-3333-333333333333", "XWTEAM03", "MW4", "同事任务", "明天", "IN_PROGRESS", 40, "同事处理中");
        addOrder("44444444-4444-4444-4444-444444444444", "XWQUEUE04", "MW4", "跨中心队列", "明天", "WAITING_CONFIRMATION", null, "待领取");
        addOrder("55555555-5555-5555-5555-555555555555", "XWGLOBAL", "MW4", "全局未归属", "本周", "WAITING_CONFIRMATION", null, "未归属");
        addOrder("66666666-6666-6666-6666-666666666666", "XWSELF03", "MW4", "SELF任务", "本周", "IN_PROGRESS", 50, "本人处理中");

        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XWMINE03',2,3,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XWQUEUE03',NULL,3,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XWTEAM03',4,3,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XWQUEUE04',NULL,4,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO xw_business_ownership VALUES ('XWSELF03',3,3,'admin','admin',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");

        XingweiPermissionGuard guard = new XingweiPermissionGuard(jdbc);
        XingweiBusinessScopeService business = new XingweiBusinessScopeService(jdbc, guard, mock(AuditService.class));
        orders = mock(OrderService.class);
        work = new XingweiWorkService(jdbc, guard, business, orders);
    }

    @Test
    void departmentWorkShowsMineAndDepartmentQueueButNotTeammatesPersonalTask() {
        Map<String, Object> result = work.snapshot(authentication("dept", "ROLE_OPERATOR"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

        assertEquals("DEPARTMENT", result.get("dataScope"));
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(row -> "XWMINE03".equals(row.get("businessNo")) && "MINE".equals(row.get("bucket"))));
        assertTrue(items.stream().anyMatch(row -> "XWQUEUE03".equals(row.get("businessNo")) && "DEPARTMENT_QUEUE".equals(row.get("bucket"))));
        assertFalse(items.stream().anyMatch(row -> "XWTEAM03".equals(row.get("businessNo"))));
        assertFalse(items.stream().anyMatch(row -> "XWQUEUE04".equals(row.get("businessNo"))));
        assertFalse(items.stream().anyMatch(row -> "XWGLOBAL".equals(row.get("businessNo"))));
        assertEquals(1L, result.get("mineCount"));
        assertEquals(1L, result.get("queueCount"));
    }

    @Test
    void selfWorkShowsOnlyOwnTask() {
        Map<String, Object> result = work.snapshot(authentication("self", "ROLE_OPERATOR"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

        assertEquals("SELF", result.get("dataScope"));
        assertEquals(1, items.size());
        assertEquals("XWSELF03", items.getFirst().get("businessNo"));
        assertEquals("MINE", items.getFirst().get("bucket"));
        assertTrue((Boolean) items.getFirst().get("canUpdate"));
        assertFalse((Boolean) items.getFirst().get("canClaim"));
    }

    @Test
    void adminWorkCanSeeCenterQueuesAndGlobalUnassigned() {
        Map<String, Object> result = work.snapshot(authentication("admin", "ROLE_ADMIN"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

        assertEquals("ALL", result.get("dataScope"));
        assertTrue(items.stream().anyMatch(row -> "XWQUEUE03".equals(row.get("businessNo"))));
        assertTrue(items.stream().anyMatch(row -> "XWQUEUE04".equals(row.get("businessNo"))));
        assertTrue(items.stream().anyMatch(row -> "XWGLOBAL".equals(row.get("businessNo")) && "UNASSIGNED".equals(row.get("bucket"))));
        assertFalse(items.stream().anyMatch(row -> "XWTEAM03".equals(row.get("businessNo"))));
    }

    @Test
    void departmentDispatcherCanClaimItsQueueAndTaskBecomesMine() {
        Authentication dept = authentication("dept", "ROLE_OPERATOR");
        Map<String, Object> ownership = work.claim("XWQUEUE03", dept, null);

        assertEquals(2L, ownership.get("ownerAdminUserId"));
        assertEquals("03", ownership.get("departmentCode"));
        Map<String, Object> refreshed = work.snapshot(dept);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) refreshed.get("items");
        assertTrue(items.stream().anyMatch(row -> "XWQUEUE03".equals(row.get("businessNo")) && "MINE".equals(row.get("bucket"))));
    }

    @Test
    void employeeCannotUpdateTeammateTaskThroughWorkEndpoint() {
        ApiDtos.UpdateOrderRequest update = new ApiDtos.UpdateOrderRequest(
                0L, "IN_PROGRESS", 60, "继续执行", null, null, "更新进度");

        assertThrows(
                AccessDeniedException.class,
                () -> work.updateOrder("XWTEAM03", update, authentication("dept", "ROLE_OPERATOR"), null));
        verifyNoInteractions(orders);
    }

    private void addOrder(
            String quoteId,
            String businessNo,
            String game,
            String demand,
            String expectedTime,
            String status,
            Integer percent,
            String progress) {
        jdbc.update(
                "INSERT INTO quotes VALUES (CAST(? AS UUID),?,?,?,?)",
                quoteId,
                businessNo,
                game,
                demand,
                expectedTime);
        jdbc.update(
                "INSERT INTO customer_orders VALUES (?,?,CAST(? AS UUID),?,?,?,?,?,?,CURRENT_TIMESTAMP,0)",
                businessNo,
                businessNo,
                quoteId,
                status,
                percent,
                progress,
                null,
                null,
                null);
    }

    private static Authentication authentication(String username, String authority) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }
}
