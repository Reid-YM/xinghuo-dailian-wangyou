package com.xinghuo.deploy;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class XingweiOperationsPermissionMigrationTest {
    @Test void exactFoundationAndNewMigrationGiveOnlyManagementTheNewPermissions() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:ops_migration"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");var jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY)");
        try(var connection=source.getConnection()){
            ScriptUtils.executeSqlScript(connection,new EncodedResource(new ClassPathResource("db/migration/V63__xingwei_org_rbac_foundation.sql"),"UTF-8"));
            ScriptUtils.executeSqlScript(connection,new EncodedResource(new ClassPathResource("db/migration/V76__xingwei_customer_feedback_permissions.sql"),"UTF-8"));
        }
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM xw_permissions WHERE code LIKE 'customer:%' OR code LIKE 'feedback:%'",Integer.class));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM xw_role_permissions rp JOIN xw_rbac_roles r ON r.id=rp.role_id JOIN xw_permissions p ON p.id=rp.permission_id WHERE r.code='XW_MANAGEMENT' AND (p.code LIKE 'customer:%' OR p.code LIKE 'feedback:%')",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM xw_role_permissions rp JOIN xw_rbac_roles r ON r.id=rp.role_id JOIN xw_permissions p ON p.id=rp.permission_id WHERE r.code<>'XW_MANAGEMENT' AND (p.code LIKE 'customer:%' OR p.code LIKE 'feedback:%')",Integer.class));
    }
}
