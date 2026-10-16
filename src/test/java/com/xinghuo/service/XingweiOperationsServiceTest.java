package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.web.AccountAdminApiController;
import com.xinghuo.account.web.AccountAdminApiController.*;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class XingweiOperationsServiceTest {
    JdbcTemplate jdbc;
    XingweiPermissionGuard guard;
    AccountAdminApiController accounts;
    AccountSessionService sessions;
    AuditService audits;
    AdminTestFeedbackService feedback;
    SlotService slots;
    UploadService uploads;
    QuoteService quotes;
    XingweiOperationsService work;
    Authentication admin, manager, department, self, readOnly;

    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:operations"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE customer_accounts(id BIGINT PRIMARY KEY,public_user_id VARCHAR(80),email VARCHAR(160),email_verified BOOLEAN,display_name VARCHAR(80),role VARCHAR(24),status VARCHAR(24),created_at TIMESTAMP(6),updated_at TIMESTAMP(6),last_login_at TIMESTAMP(6),password_hash VARCHAR(255))");
        jdbc.update("INSERT INTO customer_accounts VALUES(1,'XH-1','test@example.invalid',FALSE,'100% 用户','BUYER','ACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,NULL,'DO-NOT-EXPOSE')");
        jdbc.execute("CREATE TABLE xw_departments(id BIGINT PRIMARY KEY,code VARCHAR(16),name VARCHAR(80))");
        jdbc.update("INSERT INTO xw_departments VALUES(3,'03','本中心'),(4,'04','其他中心')");
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY,username VARCHAR(80))");
        jdbc.execute("CREATE TABLE xw_admin_profiles(admin_user_id BIGINT PRIMARY KEY,display_name VARCHAR(80))");
        jdbc.execute("CREATE TABLE xw_business_ownership(business_no VARCHAR(64) PRIMARY KEY,department_id BIGINT,owner_admin_user_id BIGINT)");
        jdbc.execute("CREATE TABLE quotes(id VARCHAR(36) PRIMARY KEY,business_no VARCHAR(64),game VARCHAR(100),demand VARCHAR(100),current_progress VARCHAR(100),customer_contact VARCHAR(100),contact_method VARCHAR(24),status VARCHAR(24),lead_score INT,lead_intent_level VARCHAR(24),next_action VARCHAR(240),follow_up_note VARCHAR(5000),ai_suggestion VARCHAR(5000),last_contacted_at TIMESTAMP,first_touch_source VARCHAR(80),utm_source VARCHAR(80),created_at TIMESTAMP,updated_at TIMESTAMP(6),customer_ip_hash VARCHAR(100))");
        for(int i=1;i<=3;i++){
            jdbc.update("INSERT INTO quotes(id,business_no,game,demand,status,created_at,updated_at,customer_ip_hash) VALUES(?,?,'游戏','需求','NEW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'PRIVATE-IP-HASH')",UUID.randomUUID().toString(),"B-"+i);
            jdbc.update("INSERT INTO xw_business_ownership VALUES(?,?,?)","B-"+i,i==3?4:3,i);
        }
        jdbc.execute("CREATE TABLE user_submission(id BIGINT PRIMARY KEY,updated_at TIMESTAMP(6))");
        jdbc.update("INSERT INTO user_submission VALUES(1,CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE game_guides(slug VARCHAR(140),status VARCHAR(24))");
        jdbc.execute("CREATE TABLE hot_slots(slot_no INT PRIMARY KEY,updated_at TIMESTAMP(6))");
        jdbc.update("INSERT INTO hot_slots VALUES(1,CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE admin_test_feedback(id BIGINT PRIMARY KEY,updated_at TIMESTAMP(6))");
        jdbc.update("INSERT INTO admin_test_feedback VALUES(1,CURRENT_TIMESTAMP)");
        guard=mock(XingweiPermissionGuard.class); accounts=mock(AccountAdminApiController.class);
        sessions=mock(AccountSessionService.class); audits=mock(AuditService.class); feedback=mock(AdminTestFeedbackService.class);
        slots=mock(SlotService.class);uploads=mock(UploadService.class);quotes=mock(QuoteService.class);
        admin=actor("admin",1,"ALL",true,true);manager=actor("manager",1,"ALL",true,false);
        department=actor("dept",1,"DEPARTMENT",true,false);self=actor("self",1,"SELF",true,false);readOnly=actor("readonly",1,"ALL",false,false);
        var business=new XingweiBusinessScopeService(jdbc,guard,audits);
        var target=new XingweiOperationsService(jdbc,guard,accounts,feedback,slots,mock(ServiceCatalogService.class),quotes,mock(QuoteRepository.class),business,uploads,audits,sessions,mock(EntityManager.class),new ObjectMapper().findAndRegisterModules());
        var tx=new TransactionInterceptor();tx.setTransactionManager(new DataSourceTransactionManager(ds));tx.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory=new ProxyFactory(target);factory.addAdvice(tx);work=(XingweiOperationsService)factory.getProxy();
        when(accounts.updateUser(eq(1L),any())).thenAnswer(call->{UserUpdateRequest b=call.getArgument(1);jdbc.update("UPDATE customer_accounts SET display_name=?,role=?,status=?,updated_at=DATEADD('SECOND',1,updated_at) WHERE id=1",b.displayName(),b.role(),b.status());return memberDto(b.role(),b.status());});
        when(accounts.resetUserPassword(eq(1L),any())).thenAnswer(call->{jdbc.update("UPDATE customer_accounts SET password_hash='NEW-HASH',updated_at=DATEADD('SECOND',1,updated_at) WHERE id=1");return com.xinghuo.account.dto.AccountDtos.OkResponse.of("ok");});
    }
    Authentication actor(String name,long id,String scope,boolean write,boolean override){
        Authentication a=new UsernamePasswordAuthenticationToken(name,"",List.of());
        when(guard.resolve(a)).thenReturn(new AccessSnapshot("SHADOW_RBAC",override,List.of(),List.of(),scope,id,"03"));
        when(guard.has(eq(a),anyString())).thenAnswer(call->!call.getArgument(1,String.class).endsWith(":manage")||write);return a;
    }
    UserAdminItem memberDto(String role,String status){return new UserAdminItem(1L,"XH-1","test@example.invalid",false,"测试用户",role,status,Instant.now(),Instant.now(),null);}
    @Test void crmIntentAndBothSourceColumnsStayInsideAllDepartmentAndSelfScope(){
        jdbc.update("UPDATE quotes SET lead_intent_level='HIGH',first_touch_source='search',utm_source='campaign'");
        assertEquals(3L,work.leads(null,null,"HIGH","search",0,admin).total());
        assertEquals(2L,work.leads(null,null,"HIGH","campaign",0,department).total());
        assertEquals(1L,work.leads(null,null,"HIGH","search",0,self).total());
        assertEquals(0L,work.leads(null,null,"LOW","search",0,admin).total());
        assertEquals(0L,work.leads(null,null,"HIGH","%",0,admin).total());
        assertThrows(com.xinghuo.exception.BadRequestException.class,()->work.leads(null,null,"INVALID",null,0,admin));
        assertThrows(com.xinghuo.exception.BadRequestException.class,()->work.leads(null,null,null,"x".repeat(81),0,admin));
    }
    @Test void globalModulesRejectNarrowScopeAndReadOnlyWritesBeforeDelegation(){
        assertThrows(AccessDeniedException.class,()->work.members("","",0,department));
        assertThrows(AccessDeniedException.class,()->work.comments("PENDING",0,self));
        assertThrows(AccessDeniedException.class,()->work.display(self));
        assertThrows(AccessDeniedException.class,()->work.feedback("","",0,department));
        assertThrows(AccessDeniedException.class,()->work.createMember(new UserCreateRequest("x@example.invalid","测试","placeholder","BUYER",false),readOnly,null));
        verifyNoInteractions(accounts,feedback,slots,uploads);
    }
    @Test void memberSearchEscapesWildcardsAndNeverReturnsCredentials(){
        var result=work.members("%","",0,admin);
        assertEquals(1,result.total());assertEquals(1,result.items().size());
        assertFalse(result.toString().contains("DO-NOT-EXPOSE"));assertFalse(result.toString().contains("password"));
        assertThrows(BadRequestException.class,()->work.members("x".repeat(121),"",0,admin));
        assertThrows(BadRequestException.class,()->work.members("","",-1,admin));
    }
    @Test void memberLockRevokesSessionsAndStaleRevisionCannotOverwrite(){
        var before=work.member(1,admin);var body=new UserUpdateRequest("停用用户","BUYER","DISABLED");
        var saved=work.updateMember(1,body,before.revision(),admin,null);
        assertNotEquals(before.revision(),saved.revision());verify(sessions).revokeAll(1L);
        assertThrows(OptimisticLockingFailureException.class,()->work.updateMember(1,new UserUpdateRequest("覆盖","BUYER","ACTIVE"),before.revision(),admin,null));
        assertEquals("DISABLED",jdbc.queryForObject("SELECT status FROM customer_accounts WHERE id=1",String.class));
        verify(accounts,times(1)).updateUser(eq(1L),any());
    }
    @Test void auditFailureRollsBackMemberAndPasswordUpdates(){
        var before=work.member(1,admin);doThrow(new IllegalStateException("audit unavailable")).when(audits).log(any(),any(),any(),any(),any(),any(),any(),any());
        assertThrows(IllegalStateException.class,()->work.updateMember(1,new UserUpdateRequest("不应保留","BUYER","ACTIVE"),before.revision(),admin,null));
        assertEquals("100% 用户",jdbc.queryForObject("SELECT display_name FROM customer_accounts WHERE id=1",String.class));
        assertThrows(IllegalStateException.class,()->work.resetMemberPassword(1,new PasswordResetRequest("new value only fixture"),before.revision(),admin,null));
        assertEquals("DO-NOT-EXPOSE",jdbc.queryForObject("SELECT password_hash FROM customer_accounts WHERE id=1",String.class));
    }
    @Test void nonOverrideCannotModifyExistingElevatedMemberOrDelete(){
        jdbc.update("UPDATE customer_accounts SET role='ADMIN' WHERE id=1");var before=work.member(1,manager);
        assertThrows(AccessDeniedException.class,()->work.updateMember(1,new UserUpdateRequest("降级","BUYER","ACTIVE"),before.revision(),manager,null));
        assertThrows(AccessDeniedException.class,()->work.resetMemberPassword(1,new PasswordResetRequest("fixture"),before.revision(),manager,null));
        assertThrows(AccessDeniedException.class,()->work.deleteMember(1,"XH-1",before.revision(),manager,null));
        assertThrows(BadRequestException.class,()->work.deleteMember(1,"WRONG",before.revision(),admin,null));
        verifyNoInteractions(accounts,sessions);
    }
    @Test void crmScopeAppliesBeforeCountAndPagingAndDoesNotLeakIpHash(){
        assertEquals(3,work.leads("","",0,admin).total());assertEquals(2,work.leads("","",0,department).total());
        var mine=work.leads("","",0,self);assertEquals(1,mine.total());assertTrue(mine.toString().contains("B-1"));assertFalse(mine.toString().contains("PRIVATE-IP-HASH"));
        assertThrows(AccessDeniedException.class,()->work.lead("B-3",self));
        assertThrows(AccessDeniedException.class,()->work.updateLead("B-3",null,"stale",true,department,null));
        verifyNoInteractions(quotes);
    }
    @Test void submissionCannotBeMarkedPublishedWithoutAnActualPublishedGuide(){
        when(accounts.submission(1L)).thenReturn(null);var before=work.submission(1,admin);
        assertThrows(BadRequestException.class,()->work.publishSubmission(1,new PublishRequest("does-not-exist"),before.revision(),admin,null));
        verify(accounts,never()).publishSubmission(anyLong(),any(),any());
    }
    @Test void attachmentAndSlotRejectStaleRevisionBeforeStorageOrMutation(){
        assertThrows(OptimisticLockingFailureException.class,()->work.updateDisplay(new ApiDtos.SlotUpdate(1,null,"AUTO","fixture"),"old",admin,null));
        assertThrows(OptimisticLockingFailureException.class,()->work.feedbackImage(1,null,"old",admin,null));
        verifyNoInteractions(slots,uploads,feedback);
    }
}
