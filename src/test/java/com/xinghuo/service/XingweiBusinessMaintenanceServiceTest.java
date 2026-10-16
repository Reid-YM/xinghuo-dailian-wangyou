package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.ConflictException;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class XingweiBusinessMaintenanceServiceTest {
    JdbcTemplate jdbc;XingweiBusinessMaintenanceService work;XingweiPermissionGuard guard;QuoteService quotes;OrderService orders;AuditService audit;
    Authentication admin,dept,self,viewer;String quoteId=UUID.randomUUID().toString();
    @BeforeEach void setup(){
        var ds=new DriverManagerDataSource("jdbc:h2:mem:maintenance"+System.nanoTime()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY,username VARCHAR(80))");jdbc.execute("CREATE TABLE xw_admin_profiles(admin_user_id BIGINT PRIMARY KEY,display_name VARCHAR(80))");jdbc.execute("CREATE TABLE xw_departments(id BIGINT PRIMARY KEY,code VARCHAR(16),name VARCHAR(80))");jdbc.update("INSERT INTO xw_departments VALUES(3,'03','本中心'),(4,'04','其他中心')");jdbc.execute("CREATE TABLE xw_business_ownership(business_no VARCHAR(64) PRIMARY KEY,department_id BIGINT,owner_admin_user_id BIGINT)");
        jdbc.execute("CREATE TABLE customer_orders(order_no VARCHAR(64) PRIMARY KEY,business_no VARCHAR(64),status VARCHAR(32),allow_public_case BOOLEAN,public_summary VARCHAR(240),created_at TIMESTAMP,updated_at TIMESTAMP(6))");
        jdbc.execute("CREATE TABLE quotes(id VARCHAR(36) PRIMARY KEY,business_no VARCHAR(64),status VARCHAR(32),created_at TIMESTAMP,updated_at TIMESTAMP(6))");
        for(int i=1;i<=3;i++){jdbc.update("INSERT INTO xw_business_ownership VALUES(?,?,?)","B-"+i,i==3?4:3,i);jdbc.update("INSERT INTO customer_orders VALUES(?,?,'COMPLETED',FALSE,NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)","O-"+i,"B-"+i);}
        jdbc.update("INSERT INTO quotes VALUES(?,'Q-1','NEW',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",quoteId);jdbc.update("INSERT INTO xw_business_ownership VALUES('Q-1',3,1)");
        guard=mock(XingweiPermissionGuard.class);quotes=mock(QuoteService.class);orders=mock(OrderService.class);audit=mock(AuditService.class);var entities=mock(EntityManager.class);
        admin=actor("admin","ALL",true,true);dept=actor("dept","DEPARTMENT",true,false);self=actor("self","SELF",true,false);viewer=actor("viewer","ALL",false,false);
        when(orders.setPublicCase(anyString(),anyBoolean(),any(),anyString())).thenAnswer(call->{jdbc.update("UPDATE customer_orders SET allow_public_case=?,public_summary=?,updated_at=DATEADD('SECOND',1,updated_at) WHERE order_no=?",call.getArgument(1),call.getArgument(2),call.getArgument(0));return Map.of();});
        var target=new XingweiBusinessMaintenanceService(jdbc,guard,new XingweiBusinessScopeService(jdbc,guard,audit),new XingweiCatalogRevisionService(jdbc,entities),quotes,orders,entities,audit);var tx=new TransactionInterceptor();tx.setTransactionManager(new DataSourceTransactionManager(ds));tx.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());var factory=new ProxyFactory(target);factory.addAdvice(tx);work=(XingweiBusinessMaintenanceService)factory.getProxy();
    }
    Authentication actor(String name,String scope,boolean write,boolean override){Authentication actor=new UsernamePasswordAuthenticationToken(name,"",List.of());when(guard.resolve(actor)).thenReturn(new AccessSnapshot("SHADOW_RBAC",override,List.of(),List.of(),scope,1L,"03"));when(guard.has(eq(actor),anyString())).thenAnswer(c->!c.getArgument(1,String.class).endsWith(":manage")||write);return actor;}
    @SuppressWarnings("unchecked") XingweiBusinessMaintenanceService.State first(String type,Authentication actor){return ((List<XingweiBusinessMaintenanceService.State>)work.snapshot(type,"",0,actor).get("items")).getFirst();}
    @Test void recordScopeIsAppliedBeforePaginationAndCounts(){assertEquals(3L,work.snapshot("orders","",0,admin).get("total"));assertEquals(2L,work.snapshot("orders","",0,dept).get("total"));assertEquals(1L,work.snapshot("orders","",0,self).get("total"));assertEquals(0L,work.snapshot("orders","%",0,admin).get("total"));}
    @Test void publicationNeedsScopePrivacyConfirmationAndFreshRevision(){
        var before=first("orders",self);var body=new XingweiBusinessMaintenanceService.PublicCaseInput(true,"已脱敏交付",true,"客户已授权");
        assertThrows(AccessDeniedException.class,()->work.publicCase("O-3",body,"old",self,null));assertThrows(AccessDeniedException.class,()->work.publicCase("O-1",body,before.revision(),viewer,null));assertThrows(BadRequestException.class,()->work.publicCase("O-1",new XingweiBusinessMaintenanceService.PublicCaseInput(true,"摘要",false,"说明"),before.revision(),self,null));verifyNoInteractions(orders);
        var after=work.publicCase("O-1",body,before.revision(),self,null);assertEquals(true,after.data().get("allowPublicCase"));assertNotEquals(before.revision(),after.revision());assertThrows(OptimisticLockingFailureException.class,()->work.publicCase("O-1",body,before.revision(),self,null));
    }
    @Test void failedPublicationAuditRollsBackPublicFlag(){
        var before=first("orders",self);doThrow(new IllegalStateException("audit unavailable")).when(audit).log(any(),any(),any(),any(),any(),any(),any(),any());assertThrows(IllegalStateException.class,()->work.publicCase("O-1",new XingweiBusinessMaintenanceService.PublicCaseInput(true,"摘要",true,"说明"),before.revision(),self,null));assertFalse(jdbc.queryForObject("SELECT allow_public_case FROM customer_orders WHERE order_no='O-1'",Boolean.class));
    }
    @Test void destructiveOperationsRequireAdminExactBusinessNumberAndVersionAndReuseDependencyGuards(){
        var quote=first("quotes",admin);assertThrows(AccessDeniedException.class,()->work.delete("quotes",quoteId,new XingweiBusinessMaintenanceService.DeleteInput("Q-1"),quote.revision(),dept,null));assertThrows(BadRequestException.class,()->work.delete("quotes",quoteId,new XingweiBusinessMaintenanceService.DeleteInput("WRONG"),quote.revision(),admin,null));assertThrows(OptimisticLockingFailureException.class,()->work.delete("quotes",quoteId,new XingweiBusinessMaintenanceService.DeleteInput("Q-1"),"old",admin,null));verifyNoInteractions(quotes);
        work.delete("quotes",quoteId,new XingweiBusinessMaintenanceService.DeleteInput("Q-1"),quote.revision(),admin,null);verify(quotes).delete(UUID.fromString(quoteId),"admin",null);
        var order=first("orders",self);doThrow(new ConflictException("订单已有支付记录，不能删除")).when(orders).delete("O-1","admin",null);assertThrows(ConflictException.class,()->work.delete("orders","O-1",new XingweiBusinessMaintenanceService.DeleteInput("B-1"),order.revision(),admin,null));assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM customer_orders",Integer.class));
    }
}
