package com.xinghuo.wecom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.xinghuo.service.AuditService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

class XingweiWeComHrAccessServiceTest {
    @Test void disableInvalidatesPasswordAndSsoStateWithoutDeletingBindingOrNotes() {
        var jdbc=database();var audit=mock(AuditService.class);var service=service(jdbc,audit);
        service.beginDisable("member-staff","manager",null);
        assertEquals(false,jdbc.queryForObject("SELECT enabled FROM admin_users WHERE id=2",Boolean.class));
        assertEquals(8L,jdbc.queryForObject("SELECT auth_version FROM admin_users WHERE id=2",Long.class));
        assertEquals("INACTIVE",jdbc.queryForObject("SELECT status FROM xw_admin_profiles WHERE admin_user_id=2",String.class));
        assertEquals("member-staff",jdbc.queryForObject("SELECT wecom_user_id FROM xw_admin_profiles WHERE admin_user_id=2",String.class));
        assertEquals("历史内部备注",jdbc.queryForObject("SELECT notes FROM xw_admin_profiles WHERE admin_user_id=2",String.class));
        assertEquals(true,jdbc.queryForObject("SELECT enabled FROM admin_users WHERE id=1",Boolean.class));
        verify(audit).log(eq("XW_WECOM_DISABLE_REQUEST"),eq("WECOM_MEMBER"),eq("member-staff"),eq("manager"),isNull(),eq(java.util.Map.of("status","后台访问已停用")),anyString(),isNull());
    }
    @Test void selfAndAdministratorCannotBeDisabledAndNoMutationOccurs() {
        var jdbc=database();var service=service(jdbc,mock(AuditService.class));
        assertThrows(ResponseStatusException.class,()->service.beginDisable("member-admin","manager",null));
        assertThrows(ResponseStatusException.class,()->service.beginDisable("member-staff","staff",null));
        assertEquals(2L,jdbc.queryForObject("SELECT COUNT(*) FROM admin_users WHERE enabled=TRUE",Long.class));
        assertEquals(7L,jdbc.queryForObject("SELECT auth_version FROM admin_users WHERE id=2",Long.class));
    }
    @Test void auditFailureRollsBackLocalRevocationBeforeAnyRemoteWriteCanStart() {
        var jdbc=database();var audit=mock(AuditService.class);var service=service(jdbc,audit);
        doThrow(new IllegalStateException("audit unavailable")).when(audit).log(anyString(),anyString(),anyString(),anyString(),any(),any(),anyString(),any());
        assertThrows(IllegalStateException.class,()->service.beginDisable("member-staff","manager",null));
        assertEquals(true,jdbc.queryForObject("SELECT enabled FROM admin_users WHERE id=2",Boolean.class));
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM xw_admin_profiles WHERE admin_user_id=2",String.class));
        assertEquals(7L,jdbc.queryForObject("SELECT auth_version FROM admin_users WHERE id=2",Long.class));
    }
    private XingweiWeComHrAccessService service(JdbcTemplate jdbc,AuditService audit) {
        var factory=new org.springframework.aop.framework.ProxyFactory(new XingweiWeComHrAccessService(jdbc,audit));
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()),
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        return (XingweiWeComHrAccessService)factory.getProxy();
    }
    private JdbcTemplate database() {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:wecom_hr_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY,username VARCHAR(64),role VARCHAR(16),enabled BOOLEAN,auth_version BIGINT,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE xw_admin_profiles(admin_user_id BIGINT,wecom_user_id VARCHAR(64),status VARCHAR(24),notes VARCHAR(200),updated_at TIMESTAMP)");
        jdbc.update("INSERT INTO admin_users VALUES(1,'admin','ADMIN',TRUE,1,NOW()),(2,'staff','OPERATOR',TRUE,7,NOW())");
        jdbc.update("INSERT INTO xw_admin_profiles VALUES(1,'member-admin','ACTIVE','管理员备注',NOW()),(2,'member-staff','ACTIVE','历史内部备注',NOW())");
        return jdbc;
    }
}
