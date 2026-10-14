package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.xinghuo.service.AdminUserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class XingweiWeComBindingServiceTest {
    @Test
    void firstEnrollmentCreatesIdentityWithoutBusinessRole() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:wecom_pending_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE admin_users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) UNIQUE,
                    password_hash VARCHAR(255), role VARCHAR(32), enabled BOOLEAN,
                    created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE xw_admin_profiles (
                    admin_user_id BIGINT PRIMARY KEY, display_name VARCHAR(80),
                    status VARCHAR(24), wecom_user_id VARCHAR(64) UNIQUE,
                    created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE xw_admin_roles (admin_user_id BIGINT, role_id BIGINT)");
        var service = new XingweiWeComBindingService(
                jdbc, mock(AdminUserService.class), new BCryptPasswordEncoder(4));

        long id = service.registerPending("staff_1", "员工甲");

        assertEquals(id, service.boundAdminUserId("staff_1"));
        var account = jdbc.queryForMap("SELECT * FROM admin_users WHERE id = ?", id);
        assertTrue(((String) account.get("USERNAME")).startsWith("wecom_"));
        assertTrue(((String) account.get("PASSWORD_HASH")).startsWith("$2"));
        assertEquals("OPERATOR", account.get("ROLE"));
        assertEquals(true, account.get("ENABLED"));
        assertEquals("ACTIVE", jdbc.queryForObject(
                "SELECT status FROM xw_admin_profiles WHERE admin_user_id = ?", String.class, id));
        assertNotNull(jdbc.queryForObject(
                "SELECT display_name FROM xw_admin_profiles WHERE admin_user_id = ?", String.class, id));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM xw_admin_roles WHERE admin_user_id = ?", Long.class, id));
        assertThrows(IllegalArgumentException.class, () -> service.registerPending("staff_1", "员工甲"));
    }
}
