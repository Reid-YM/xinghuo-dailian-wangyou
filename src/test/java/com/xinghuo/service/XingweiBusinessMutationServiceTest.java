package com.xinghuo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.model.Quote;
import com.xinghuo.model.CustomerOrder;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import com.xinghuo.service.XingweiBusinessMutationService.QuoteChange;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class XingweiBusinessMutationServiceTest {
    private XingweiBusinessMutationService service;
    private XingweiPermissionGuard guard;
    private QuoteRepository quoteRepo;
    private CustomerOrderRepository orderRepo;
    private QuoteService quotes;
    private OrderService orders;
    private UploadService uploads;
    private Authentication admin,dept,self,view;
    private Quote quote;
    private final UUID quoteId=UUID.randomUUID();
    @BeforeEach
    void setup(){
        JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:mutation"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE quotes(business_no VARCHAR(40) PRIMARY KEY)");jdbc.execute("CREATE TABLE customer_orders(business_no VARCHAR(40),order_no VARCHAR(40))");
        jdbc.execute("CREATE TABLE xw_business_ownership(business_no VARCHAR(40) PRIMARY KEY,owner_admin_user_id BIGINT,department_id BIGINT)");
        jdbc.execute("CREATE TABLE admin_users(id BIGINT PRIMARY KEY,username VARCHAR(64))");jdbc.execute("CREATE TABLE xw_admin_profiles(admin_user_id BIGINT PRIMARY KEY,display_name VARCHAR(80))");
        jdbc.execute("CREATE TABLE xw_departments(id BIGINT PRIMARY KEY,code VARCHAR(16),name VARCHAR(80))");
        jdbc.update("INSERT INTO admin_users VALUES(2,'dept'),(3,'self'),(4,'other')");jdbc.update("INSERT INTO xw_departments VALUES(3,'03','客服'),(4,'04','游戏')");
        jdbc.update("INSERT INTO quotes VALUES('XWOWN'),('XWOTHER')");jdbc.update("INSERT INTO customer_orders VALUES('XWOWN','ORDER1'),('XWOTHER','ORDER2')");
        jdbc.update("INSERT INTO xw_business_ownership VALUES('XWOWN',3,3),('XWOTHER',4,4)");
        guard=mock(XingweiPermissionGuard.class);admin=actor("admin",1L,"ALL",true);dept=actor("dept",2L,"DEPARTMENT",true);self=actor("self",3L,"SELF",true);view=actor("viewer",5L,"ALL",false);
        quoteRepo=mock(QuoteRepository.class);orderRepo=mock(CustomerOrderRepository.class);quotes=mock(QuoteService.class);orders=mock(OrderService.class);uploads=mock(UploadService.class);
        quote=new Quote();quote.setId(quoteId);quote.setBusinessNo("XWOWN");quote.setStatus(Quote.Status.NEW);when(quoteRepo.findByBusinessNo("XWOWN")).thenReturn(Optional.of(quote));
        var scope=new XingweiBusinessScopeService(jdbc,guard,mock(AuditService.class));
        service=new XingweiBusinessMutationService(jdbc,guard,scope,quoteRepo,orderRepo,quotes,orders,uploads);
    }
    private Authentication actor(String name,Long id,String scope,boolean manage){
        Authentication a=new UsernamePasswordAuthenticationToken(name,"",List.of());
        when(guard.resolve(a)).thenReturn(new AccessSnapshot("SHADOW_RBAC",false,List.of(),List.of(),scope,id,"03"));
        for(String permission:List.of("quote:manage","order:manage"))when(guard.has(a,permission)).thenReturn(manage);
        when(guard.has(a,"quote:view")).thenReturn(true);when(guard.has(a,"order:view")).thenReturn(true);return a;
    }
    @Test
    void realOwnershipScopeBlocksCrossDepartmentAndSelfMutations(){
        var body=new QuoteChange("CONTACTED","NEW","跟进");service.updateQuote("XWOWN",body,self,null);
        verify(quotes).updateStatus(eq(quoteId),any(),eq("self"),isNull());
        assertThrows(AccessDeniedException.class,()->service.updateQuote("XWOTHER",body,self,null));
        assertThrows(AccessDeniedException.class,()->service.updateQuote("XWOTHER",body,dept,null));
        assertThrows(AccessDeniedException.class,()->service.updateQuote("XWOWN",body,view,null));
        assertThrows(AccessDeniedException.class,()->service.quoteImage("XWOTHER",self));
    }
    @Test
    void quoteStatusVersionPreventsStaleOverwriteAndFakeConversion(){
        assertThrows(OptimisticLockingFailureException.class,()->service.updateQuote("XWOWN",new QuoteChange("QUOTED","CONTACTED",""),self,null));
        assertThrows(IllegalArgumentException.class,()->service.updateQuote("XWOWN",new QuoteChange("CONVERTED","NEW",""),self,null));
        verifyNoInteractions(quotes);
    }
    @Test
    void conversionRequiresBothPermissionsAndRetriesExistingConvertedQuote(){
        when(guard.has(self,"order:manage")).thenReturn(false);
        assertThrows(AccessDeniedException.class,()->service.convert("XWOWN",new QuoteChange(null,"NEW","确认"),self,null));
        verifyNoInteractions(quotes);when(guard.has(self,"order:manage")).thenReturn(true);
        quote.setStatus(Quote.Status.CONVERTED);
        service.convert("XWOWN",new QuoteChange(null,"NEW","重试"),self,null);
        verify(quotes).convertToOrder(eq(quoteId),any(),eq("self"),isNull());
    }
    @Test
    void manualCreateIsGlobalOnlyWhileScopedOrderUpdatesUseExistingVersionedService(){
        var create=new ApiDtos.CreateOrderRequest("WAITING_CONFIRMATION",0,"待确认",null,null,"新增");
        assertThrows(AccessDeniedException.class,()->service.createOrder(create,dept,null));service.createOrder(create,admin,null);
        verify(orders).create(create,"admin",null);
        var update=new ApiDtos.UpdateOrderRequest(2L,"IN_PROGRESS",20,"处理中",null,null,"推进");service.updateOrder("XWOWN",update,dept,null);
        verify(orders).update("ORDER1",update,"dept",null);
        assertThrows(AccessDeniedException.class,()->service.updateOrder("XWOTHER",update,dept,null));
    }
    @Test
    void acceptedScreenshotFlushesBeforeReturningItsUpdatedVersion(){
        var order=mock(CustomerOrder.class);when(order.getVersion()).thenReturn(3L);when(orders.findByOrderNo("ORDER1")).thenReturn(order);
        var file=mock(org.springframework.web.multipart.MultipartFile.class);var asset=mock(com.xinghuo.model.UploadAsset.class);var id=UUID.randomUUID();when(asset.getId()).thenReturn(id);
        when(uploads.storeImage(file,com.xinghuo.model.UploadAsset.Kind.ORDER_PROGRESS)).thenReturn(asset);
        service.attachImage("XWOWN",3L,file,self,null);
        var calls=inOrder(orders,uploads,orderRepo);calls.verify(orders).findByOrderNo("ORDER1");calls.verify(uploads).storeImage(file,com.xinghuo.model.UploadAsset.Kind.ORDER_PROGRESS);
        calls.verify(orders).attachImage("ORDER1",id,"self",null);calls.verify(orderRepo).flush();calls.verify(orders).admin(order);
    }
    @Test
    void staleScreenshotIsRejectedBeforeAnyFileWrite(){
        var order=mock(CustomerOrder.class);when(order.getVersion()).thenReturn(3L);when(orders.findByOrderNo("ORDER1")).thenReturn(order);
        assertThrows(OptimisticLockingFailureException.class,()->service.attachImage("XWOWN",2L,mock(org.springframework.web.multipart.MultipartFile.class),self,null));
        verifyNoInteractions(uploads);verify(orderRepo,never()).flush();
    }
}
