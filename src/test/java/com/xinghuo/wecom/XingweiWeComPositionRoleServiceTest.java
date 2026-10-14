package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class XingweiWeComPositionRoleServiceTest {
    @Test
    void uniqueBusinessTitleGrantsCenterRoleAndUnknownTitleRevokesIt() {
        JdbcTemplate jdbc = database();
        var service = new XingweiWeComPositionRoleService(jdbc);

        assertTrue(service.sync(1, "在线客服"));
        assertEquals(3L, jdbc.queryForObject(
                "SELECT role_id FROM xw_admin_roles WHERE admin_user_id = 1", Long.class));
        assertEquals(3L, jdbc.queryForObject(
                "SELECT department_id FROM xw_admin_profiles WHERE admin_user_id = 1", Long.class));
        assertTrue(service.needsRefresh(1));

        assertTrue(service.sync(1, "未知职务"));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM xw_admin_roles WHERE admin_user_id = 1", Long.class));
    }

    @Test
    void manualRoleAndAmbiguousOrManagementTitlesAreNeverOverwritten() {
        JdbcTemplate jdbc = database();
        var service = new XingweiWeComPositionRoleService(jdbc);

        jdbc.update("INSERT INTO xw_admin_roles VALUES (1, 7, TRUE, CURRENT_TIMESTAMP)");
        assertFalse(service.needsRefresh(1));
        assertFalse(service.sync(1, "在线客服"));
        assertEquals(7L, jdbc.queryForObject(
                "SELECT role_id FROM xw_admin_roles WHERE admin_user_id = 1", Long.class));
        jdbc.update("DELETE FROM xw_admin_roles WHERE admin_user_id = 1");
        assertFalse(service.sync(1, "工作室负责人"));
        assertFalse(service.sync(1, "异常订单处理专员"));
        assertFalse(service.sync(1, "服务器运维工程师"));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM xw_admin_roles WHERE admin_user_id = 1", Long.class));
    }

    private JdbcTemplate database() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:wecom_roles_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE xw_departments (id BIGINT PRIMARY KEY, code VARCHAR(16), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE xw_positions (id BIGINT PRIMARY KEY, department_id BIGINT, name VARCHAR(80), built_in BOOLEAN, enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE xw_rbac_roles (id BIGINT PRIMARY KEY, code VARCHAR(48), enabled BOOLEAN, data_scope VARCHAR(24))");
        jdbc.execute("CREATE TABLE xw_admin_profiles (admin_user_id BIGINT PRIMARY KEY, department_id BIGINT, position_id BIGINT, status VARCHAR(24), updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE xw_admin_roles (admin_user_id BIGINT, role_id BIGINT, is_primary BOOLEAN, created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE xw_wecom_auto_roles (admin_user_id BIGINT PRIMARY KEY, position_id BIGINT, role_id BIGINT, wecom_position VARCHAR(80), updated_at TIMESTAMP)");
        jdbc.update("INSERT INTO xw_departments VALUES (1,'01',TRUE),(3,'03',TRUE),(4,'04',TRUE),(5,'05',TRUE)");
        jdbc.update("INSERT INTO xw_positions VALUES (1,1,'工作室负责人',TRUE,TRUE),(38,3,'在线客服',TRUE,TRUE),(40,3,'异常订单处理专员',TRUE,TRUE),(80,4,'异常订单处理专员',TRUE,TRUE),(100,5,'服务器运维工程师',TRUE,TRUE)");
        jdbc.update("INSERT INTO xw_rbac_roles VALUES (3,'XW_CUSTOMER_ORDER',TRUE,'DEPARTMENT'),(5,'XW_TECH_PRODUCT',TRUE,'DEPARTMENT'),(7,'XW_GENERAL_SUPPORT',TRUE,'DEPARTMENT')");
        jdbc.update("INSERT INTO xw_admin_profiles VALUES (1,NULL,NULL,'ACTIVE',CURRENT_TIMESTAMP)");
        return jdbc;
    }
}
