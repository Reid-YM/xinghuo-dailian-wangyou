package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class XingweiWeComBindingMigrationTest {
    @Test
    void v68AllowsUnboundProfilesButRejectsDuplicateUserIds() throws Exception {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V68__xingwei_wecom_user_binding.sql"));
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:wecom_binding;MODE=MySQL;DB_CLOSE_DELAY=-1")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE xw_admin_profiles (admin_user_id BIGINT PRIMARY KEY)");
                for (String sql : migration.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
                statement.execute("INSERT INTO xw_admin_profiles (admin_user_id) VALUES (1), (2), (3)");
                try (var result = statement.executeQuery(
                        "SELECT COUNT(*) FROM xw_admin_profiles WHERE wecom_user_id IS NULL")) {
                    result.next();
                    assertEquals(3, result.getInt(1));
                }
                statement.execute("UPDATE xw_admin_profiles SET wecom_user_id = 'staff_1' WHERE admin_user_id = 1");
                assertThrows(SQLException.class, () -> statement.execute(
                        "UPDATE xw_admin_profiles SET wecom_user_id = 'staff_1' WHERE admin_user_id = 2"));
            }
        }
    }
}
