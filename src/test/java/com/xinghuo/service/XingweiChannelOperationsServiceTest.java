package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import com.xinghuo.service.XingweiChannelOperationsService.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class XingweiChannelOperationsServiceTest {
    private JdbcTemplate jdbc;
    private XingweiPermissionGuard guard;
    private AuditService audits;
    private XingweiChannelOperationsService service;
    private Authentication admin, dept, self, compat, denied;

    @BeforeEach
    void setup() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:channels" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY,username VARCHAR(64),enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE xw_departments(id BIGINT PRIMARY KEY,code VARCHAR(16))");
        jdbc.execute("CREATE TABLE xw_admin_profiles(admin_user_id BIGINT PRIMARY KEY,display_name VARCHAR(80),department_id BIGINT,status VARCHAR(24))");
        jdbc.update("INSERT INTO admin_users VALUES(1,'admin',TRUE),(2,'dept',TRUE),(3,'self',TRUE),(4,'other',TRUE),(5,'disabled',FALSE)");
        jdbc.update("INSERT INTO xw_departments VALUES(3,'03'),(4,'04')");
        jdbc.update("INSERT INTO xw_admin_profiles VALUES(1,'管理员',3,'ACTIVE'),(2,'中心管理员',3,'ACTIVE'),(3,'本人',3,'ACTIVE'),(4,'跨中心员工',4,'ACTIVE')");
        new ResourceDatabasePopulator(new FileSystemResource("src/main/resources/db/migration/V75__xingwei_channel_operations.sql")).execute(ds);
        guard = mock(XingweiPermissionGuard.class);
        audits = mock(AuditService.class);
        admin = actor("admin",1L,"ALL",true,true);
        dept = actor("dept",2L,"DEPARTMENT",true,false);
        self = actor("self",3L,"SELF",true,false);
        compat = actor("compat",4L,"ALL",false,false);
        denied = actor("denied",null,"NONE",false,false);
        when(guard.resolve(denied)).thenReturn(new AccessSnapshot("DENIED",false,List.of(),List.of(),"NONE",null,null));
        var target = new XingweiChannelOperationsService(jdbc,guard,audits);
        var tx = new TransactionInterceptor();
        tx.setTransactionManager(new DataSourceTransactionManager(ds));
        tx.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory = new ProxyFactory(target); factory.addAdvice(tx);
        service = (XingweiChannelOperationsService) factory.getProxy();
    }

    private Authentication actor(String name,Long id,String scope,boolean write,boolean all) {
        Authentication actor = new UsernamePasswordAuthenticationToken(name,"",List.of());
        when(guard.resolve(actor)).thenReturn(new AccessSnapshot(all ? "LEGACY_ADMIN_OVERRIDE" : "SHADOW_RBAC",all,
                List.of("channel:view"),List.of(),scope,id,"03"));
        when(guard.has(actor,"channel:view")).thenReturn(true);
        when(guard.has(actor,"channel:manage")).thenReturn(write);
        return actor;
    }
    private TaskInput input(Long owner,String title) { return new TaskInput("TAOBAO",title,"任务说明","TODO",owner,LocalDate.of(2026,10,30),null); }
    @SuppressWarnings("unchecked")
    private List<Task> tasks(Authentication actor,String q) { return (List<Task>) service.snapshot(actor,"","",q,0).get("items"); }

    @Test void hiddenChannelNoteIsPreservedDuringStateUpdate() {
        service.updateChannel("TAOBAO",admin,new StateInput("AVAILABLE","历史渠道备注",0L),null);
        ChannelState updated=service.updateChannel("TAOBAO",admin,new StateInput("PAUSED",null,1L),null);
        assertEquals("历史渠道备注",updated.note());
        assertEquals("PAUSED",updated.serviceState());
    }
    @Test
    void databaseMigrationStartsAllChannelsUnknownAndTaskScopeIsEnforced() {
        service.create(admin,input(2L,"本中心"),null);
        service.create(admin,input(3L,"本人"),null);
        service.create(admin,input(4L,"跨中心"),null);
        assertEquals(3,tasks(admin,"").size());
        assertEquals(2,tasks(dept,"").size());
        assertEquals(1,tasks(self,"").size());
        assertEquals("本人",tasks(self,"").getFirst().title());
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM xw_channel_status WHERE service_state='UNKNOWN'",Integer.class));
        assertThrows(AccessDeniedException.class,() -> tasks(denied,""));
        assertFalse((Boolean)service.snapshot(compat,"","","",0).get("canManage"));
        assertEquals(List.of(),service.snapshot(compat,"","","",0).get("owners"));
    }

    @Test
    void departmentWithoutDepartmentFallsBackToSelfAndCannotAssignPeer() {
        when(guard.resolve(dept)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),"DEPARTMENT",2L,null));
        service.create(admin,input(2L,"自己的"),null);
        service.create(admin,input(3L,"同事的"),null);
        assertEquals(1,tasks(dept,"").size());
        assertThrows(AccessDeniedException.class,() -> service.create(dept,input(3L,"越权"),null));
    }

    @Test
    void writesCannotReachCrossScopeOrDisabledOwnersAndReadOnlyCannotWrite() {
        Task task = service.create(admin,input(4L,"跨中心"),null);
        assertThrows(AccessDeniedException.class,() -> service.update(task.id(),dept,
                new TaskInput("TAOBAO","篡改","","DONE",2L,null,0L),null));
        assertThrows(AccessDeniedException.class,() -> service.create(dept,input(4L,"跨中心"),null));
        assertThrows(AccessDeniedException.class,() -> service.create(self,input(2L,"同事"),null));
        assertThrows(IllegalArgumentException.class,() -> service.create(admin,input(5L,"停用"),null));
        assertThrows(AccessDeniedException.class,() -> service.create(compat,input(4L,"只读"),null));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM xw_channel_tasks",Integer.class));
    }

    @Test
    void versionConflictDoesNotOverwriteAndCancellationKeepsRecord() {
        Task first = service.create(self,input(null,"初始任务"),null);
        TaskInput change = new TaskInput("XIANYU","已取消任务","保留历史","CANCELLED",3L,null,0L);
        Task saved = service.update(first.id(),self,change,null);
        assertEquals(1,saved.version()); assertEquals("CANCELLED",saved.status());
        assertThrows(OptimisticLockingFailureException.class,() -> service.update(first.id(),self,change,null));
        assertEquals("已取消任务",tasks(self,"").getFirst().title());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM xw_channel_tasks",Integer.class));
        verify(audits,times(1)).log(eq("CHANNEL_TASK_UPDATE"),any(),any(),any(),any(),any(),any(),isNull());
    }

    @Test
    void auditFailureRollsBackTaskAndGlobalStateWrites() {
        doThrow(new IllegalStateException("audit unavailable")).when(audits).log(any(),any(),any(),any(),any(),any(),any(),any());
        assertThrows(IllegalStateException.class,() -> service.create(admin,input(2L,"回滚"),null));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM xw_channel_tasks",Integer.class));
        assertThrows(IllegalStateException.class,() -> service.updateChannel("TAOBAO",admin,new StateInput("PAUSED","暂停",0L),null));
        assertEquals("UNKNOWN",jdbc.queryForObject("SELECT service_state FROM xw_channel_status WHERE channel_code='TAOBAO'",String.class));
    }

    @Test
    void globalChannelStateNeedsAllScopeAndOptimisticVersion() {
        assertThrows(AccessDeniedException.class,() -> service.updateChannel("TAOBAO",dept,new StateInput("AVAILABLE","",0L),null));
        ChannelState state = service.updateChannel("TAOBAO",admin,new StateInput("AVAILABLE","人工核对可接单",0L),null);
        assertEquals("AVAILABLE",state.serviceState());
        assertThrows(OptimisticLockingFailureException.class,() -> service.updateChannel("TAOBAO",admin,new StateInput("PAUSED","",0L),null));
    }

    @Test
    void searchTreatsWildcardsAsTextAndRejectsInvalidFilters() {
        service.create(admin,input(1L,"100% 完成"),null);
        service.create(admin,input(1L,"普通任务"),null);
        assertEquals(1,tasks(admin,"%").size());
        assertThrows(IllegalArgumentException.class,() -> service.snapshot(admin,"INVALID","","",0));
        assertThrows(IllegalArgumentException.class,() -> service.snapshot(admin,"","INVALID","",0));
        assertThrows(IllegalArgumentException.class,() -> service.snapshot(admin,"","","x".repeat(81),0));
        assertThrows(IllegalArgumentException.class,() -> service.snapshot(admin,"","","",-1));
        assertThrows(IllegalArgumentException.class,() -> service.create(admin,input(1L," "),null));
    }

    @Test
    void paginationCountsAllMatchingTasksWithoutLeakingOtherScopes() {
        for (int n=0;n<31;n++) service.create(admin,input(3L,"本人任务"+n),null);
        service.create(admin,input(4L,"其他中心"),null);
        Map<String,Object> first = service.snapshot(self,"TAOBAO","TODO","",0);
        assertEquals(31L,first.get("totalCount"));
        assertEquals(30,((List<?>)first.get("items")).size());
        assertEquals(1,((List<?>)service.snapshot(self,"TAOBAO","TODO","",1).get("items")).size());
    }
}
