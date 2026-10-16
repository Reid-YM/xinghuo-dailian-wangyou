package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.xinghuo.account.service.AccountException;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import com.xinghuo.service.XingweiAdminAccountService.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class XingweiAdminAccountServiceTest {
    JdbcTemplate jdbc;XingweiPermissionGuard guard;AuditService audits;XingweiAdminAccountService work;
    BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
    Authentication first,second,manager,narrow,readonly;
    @BeforeEach void setup() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:staff"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE admin_users(id BIGINT AUTO_INCREMENT PRIMARY KEY,username VARCHAR(64) UNIQUE NOT NULL,password_hash VARCHAR(255) NOT NULL,role VARCHAR(32) NOT NULL,enabled BOOLEAN NOT NULL,created_at DATETIME(6) NOT NULL,updated_at DATETIME(6) NOT NULL)");
        for(String migration:List.of("V63__xingwei_org_rbac_foundation.sql","V68__xingwei_wecom_user_binding.sql","V69__xingwei_wecom_auto_role_tracking.sql","V77__admin_session_revision.sql")){
            try(var connection=ds.getConnection()){ScriptUtils.executeSqlScript(connection,new EncodedResource(new ClassPathResource("db/migration/"+migration),"UTF-8"));}
        }
        jdbc.update("INSERT INTO admin_users(username,password_hash,role,enabled,created_at,updated_at) VALUES('first','HASH-1','ADMIN',TRUE,NOW(6),NOW(6)),('second','HASH-2','ADMIN',TRUE,NOW(6),NOW(6)),('operator','HASH-3','OPERATOR',FALSE,NOW(6),NOW(6))");
        var org=new XingweiOrgRbacService(jdbc);org.updateEmployeeProfile(3,new XingweiOrgRbacService.ProfileInput(null,"员工","01",null,"ACTIVE",null));org.replaceEmployeeRoles(3,List.of("XW_MANAGEMENT"));
        guard=mock(XingweiPermissionGuard.class);audits=mock(AuditService.class);
        first=actor("first",1,"ALL",true,true);second=actor("second",2,"ALL",true,true);manager=actor("manager",4,"ALL",true,false);narrow=actor("narrow",5,"SELF",true,false);readonly=actor("readonly",6,"ALL",false,false);
        when(guard.canAssignEmployeeRoles(any(),anyLong(),anyList())).thenReturn(true);
        var target=new XingweiAdminAccountService(jdbc,guard,org,passwords,audits);
        var tx=new TransactionInterceptor();tx.setTransactionManager(new DataSourceTransactionManager(ds));tx.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy=new ProxyFactory(target);proxy.addAdvice(tx);work=(XingweiAdminAccountService)proxy.getProxy();
    }
    Authentication actor(String name,long id,String scope,boolean write,boolean override){Authentication a=new UsernamePasswordAuthenticationToken(name,"",List.of());when(guard.resolve(a)).thenReturn(new AccessSnapshot("SHADOW_RBAC",override,List.of(),List.of(),scope,id,"01"));when(guard.has(eq(a),anyString())).thenAnswer(c->!c.getArgument(1,String.class).endsWith(":manage")||write);return a;}
    CreateInput input(String name,List<String> roles){return new CreateInput(name,"新员工","01","Sample-Login-913!","OPERATOR",roles);}
    @Test void createsHashedIdentityWithExplicitRolesInOneTransaction(){
        work.create(input("new.staff",List.of("XW_MANAGEMENT")),first,null);
        var row=jdbc.queryForMap("SELECT id,password_hash,enabled,auth_version FROM admin_users WHERE username='new.staff'");
        assertTrue(passwords.matches("Sample-Login-913!",String.valueOf(row.get("password_hash"))));assertNotEquals("Sample-Login-913!",row.get("password_hash"));assertEquals(true,row.get("enabled"));assertEquals(0L,((Number)row.get("auth_version")).longValue());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM xw_admin_roles WHERE admin_user_id=?",Integer.class,row.get("id")));
        assertFalse(work.snapshot(first,"").toString().contains("HASH-1"));assertFalse(work.snapshot(first,"").toString().contains("password_hash"));
    }
    @Test void noRoleInvalidDepartmentOrAuditFailureCannotLeaveAnEnabledCompatibilityAccount(){
        assertThrows(BadRequestException.class,()->work.create(input("unassigned",List.of()),first,null));
        assertThrows(BadRequestException.class,()->work.create(input("bad.roles",List.of("UNKNOWN")),first,null));
        assertThrows(BadRequestException.class,()->work.create(new CreateInput("bad.dept","员工","99","Sample-Login-913!","OPERATOR",List.of("XW_MANAGEMENT")),first,null));
        doThrow(new IllegalStateException("audit unavailable")).when(audits).log(any(),any(),any(),any(),any(),any(),any(),any());
        assertThrows(IllegalStateException.class,()->work.create(input("audit.rollback",List.of("XW_MANAGEMENT")),first,null));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM admin_users",Integer.class));
    }
    @Test void stoppedAccountAndPasswordRevisionInvalidateOldStateAndRejectStaleWrites(){
        work.update(3,new UpdateInput("OPERATOR",true,0),first,null);assertEquals(1L,version(3));
        assertThrows(OptimisticLockingFailureException.class,()->work.password(3,new PasswordInput("New-Login-913!",0),first,null));
        work.password(3,new PasswordInput("New-Login-913!",1),first,null);assertEquals(2L,version(3));
        work.update(3,new UpdateInput("OPERATOR",false,2),first,null);assertEquals(3L,version(3));assertFalse(jdbc.queryForObject("SELECT enabled FROM admin_users WHERE id=3",Boolean.class));
        assertTrue(passwords.matches("New-Login-913!",jdbc.queryForObject("SELECT password_hash FROM admin_users WHERE id=3",String.class)));
    }
    @Test void protectsOwnIdentityLastAdministratorAndElevatedTargets(){
        assertThrows(BadRequestException.class,()->work.update(1,new UpdateInput("ADMIN",false,0),first,null));
        work.update(2,new UpdateInput("ADMIN",false,0),first,null);
        assertThrows(BadRequestException.class,()->work.update(1,new UpdateInput("ADMIN",false,0),second,null));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admin_users WHERE enabled=TRUE AND role='ADMIN'",Integer.class));
        assertThrows(AccessDeniedException.class,()->work.password(1,new PasswordInput("New-Login-913!",0),manager,null));
        assertThrows(AccessDeniedException.class,()->work.create(new CreateInput("elevated","员工","01","Sample-Login-913!","ADMIN",List.of()),manager,null));
    }
    @Test void concurrentCrossDisablesAlwaysKeepOneRecoveryAdministrator() throws Exception {
        var start=new CountDownLatch(1);try(var executor=Executors.newFixedThreadPool(2)){
            Future<Boolean> one=executor.submit(()->{start.await();try{work.update(2,new UpdateInput("ADMIN",false,0),first,null);return true;}catch(BadRequestException expected){return false;}});
            Future<Boolean> two=executor.submit(()->{start.await();try{work.update(1,new UpdateInput("ADMIN",false,0),second,null);return true;}catch(BadRequestException expected){return false;}});start.countDown();
            assertNotEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM admin_users WHERE enabled=TRUE AND role='ADMIN'",Integer.class));
    }
    @Test void narrowScopesReadOnlyUsersAndUnassignedReenableAreDenied(){
        assertThrows(AccessDeniedException.class,()->work.snapshot(narrow,""));
        assertThrows(AccessDeniedException.class,()->work.create(input("readonly.new",List.of("XW_MANAGEMENT")),readonly,null));
        jdbc.update("DELETE FROM xw_admin_roles WHERE admin_user_id=3");
        assertThrows(BadRequestException.class,()->work.update(3,new UpdateInput("OPERATOR",true,0),first,null));
        assertThrows(AccountException.class,()->work.password(3,new PasswordInput("short",0),first,null));
        assertEquals(0L,version(3));
    }
    @Test void auditFailureRollsBackPasswordHashAndRevision(){
        doThrow(new IllegalStateException("audit unavailable")).when(audits).log(any(),any(),any(),any(),any(),any(),any(),any());
        assertThrows(IllegalStateException.class,()->work.password(3,new PasswordInput("New-Login-913!",0),first,null));
        assertEquals("HASH-3",jdbc.queryForObject("SELECT password_hash FROM admin_users WHERE id=3",String.class));assertEquals(0L,version(3));
    }
    long version(long id){return jdbc.queryForObject("SELECT auth_version FROM admin_users WHERE id=?",Long.class,id);}
}
