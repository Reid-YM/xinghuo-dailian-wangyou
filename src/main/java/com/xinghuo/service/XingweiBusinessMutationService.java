package com.xinghuo.service;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.Quote;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.repository.CustomerOrderRepository;
import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Native business actions reuse the existing state machines and inherited business ownership. */
@Service
public class XingweiBusinessMutationService {
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard guard;
    private final XingweiBusinessScopeService scope;
    private final QuoteRepository quoteRepo;
    private final CustomerOrderRepository orderRepo;
    private final QuoteService quotes;
    private final OrderService orders;
    private final UploadService uploads;
    public XingweiBusinessMutationService(JdbcTemplate jdbc,XingweiPermissionGuard guard,XingweiBusinessScopeService scope,
        QuoteRepository quoteRepo,CustomerOrderRepository orderRepo,QuoteService quotes,OrderService orders,UploadService uploads){
        this.jdbc=jdbc;this.guard=guard;this.scope=scope;this.quoteRepo=quoteRepo;this.orderRepo=orderRepo;this.quotes=quotes;this.orders=orders;this.uploads=uploads;
    }
    @Transactional
    public ApiDtos.QuoteAdminResponse updateQuote(String no,QuoteChange change,Authentication actor,HttpServletRequest request){
        no=requireBusiness(no,actor,"quote:manage");Quote quote=lockQuote(no);
        expected(quote,change.expectedStatus());
        if(!List.of("NEW","CONTACTED","QUOTED","CLOSED","CANCELLED").contains(change.status()==null?"":change.status()))throw new IllegalArgumentException("询价状态无效；转单请使用专用操作");
        return quotes.updateStatus(quote.getId(),new ApiDtos.QuoteStatusUpdate(change.status(),change.reason()),actor.getName(),request);
    }
    @Transactional
    public ApiDtos.AdminOrderResponse convert(String no,QuoteChange change,Authentication actor,HttpServletRequest request){
        no=requireBusiness(no,actor,"quote:manage");
        if(!guard.has(actor,"order:manage"))throw new AccessDeniedException("询价转单还需要订单管理权限");
        Quote quote=lockQuote(no);
        // Existing conversion is idempotent; retrying a lost response returns the same order.
        if(quote.getStatus()!=Quote.Status.CONVERTED)expected(quote,change.expectedStatus());
        return quotes.convertToOrder(quote.getId(),new ApiDtos.QuoteConvertRequest(change.reason()),actor.getName(),request);
    }
    @Transactional
    public ApiDtos.AdminOrderResponse updateOrder(String no,ApiDtos.UpdateOrderRequest body,Authentication actor,HttpServletRequest request){
        no=requireBusiness(no,actor,"order:manage");String orderNo=lockOrder(no);
        return orders.update(orderNo,body,actor.getName(),request);
    }
    @Transactional
    public ApiDtos.AdminOrderResponse createOrder(ApiDtos.CreateOrderRequest body,Authentication actor,HttpServletRequest request){
        var access=guard.resolve(actor);
        if("DENIED".equals(access.mode())||!guard.has(actor,"order:manage")||!(access.allowAll()||"ALL".equals(access.dataScope())))
            throw new AccessDeniedException("仅全部数据范围的订单管理员可新建未归属订单；其他员工可处理范围内询价转单");
        return orders.create(body,actor.getName(),request);
    }
    @Transactional
    public ApiDtos.AdminOrderResponse attachImage(String no,long version,MultipartFile file,Authentication actor,HttpServletRequest request){
        no=requireBusiness(no,actor,"order:manage");String orderNo=lockOrder(no);var order=orders.findByOrderNo(orderNo);
        if(order.getVersion()!=version)throw new OptimisticLockingFailureException("订单已更新，请重新读取后上传截图");
        UploadAsset asset=uploads.storeImage(file,UploadAsset.Kind.ORDER_PROGRESS);
        orders.attachImage(orderNo,asset.getId(),actor.getName(),request);
        orderRepo.flush();
        return orders.admin(order);
    }
    @Transactional(readOnly=true)
    public ResponseEntity<Resource> quoteImage(String no,Authentication actor){
        no=requireBusiness(no,actor,"quote:view");Quote quote=quoteRepo.findByBusinessNo(no).orElseThrow(NotFoundException::new);
        UUID id=quote.getScreenshotAssetId();if(id==null)throw new NotFoundException();
        return ResponseEntity.ok().contentType(uploads.mediaType(id)).cacheControl(CacheControl.noStore()).body(uploads.load(id,"QUOTE",quote.getId().toString()));
    }
    @Transactional(readOnly=true)
    public ResponseEntity<Resource> orderImage(String no,Authentication actor){
        no=requireBusiness(no,actor,"order:view");var order=orders.findByPublicNo(no);UUID id=order.getProgressImageAssetId();if(id==null)throw new NotFoundException();
        return ResponseEntity.ok().contentType(uploads.mediaType(id)).cacheControl(CacheControl.noStore()).body(uploads.load(id,"ORDER",order.getOrderNo()));
    }
    private String requireBusiness(String no,Authentication actor,String permission){
        if(no==null||!no.matches("[A-Za-z0-9_-]{1,40}"))throw new IllegalArgumentException("业务编号无效");
        if("DENIED".equals(guard.resolve(actor).mode())||!guard.has(actor,permission)||!scope.canAccessBusiness(actor,no))throw new AccessDeniedException("没有对应操作权限或业务超出当前数据范围");
        return no;
    }
    private Quote lockQuote(String no){
        List<String> found=jdbc.queryForList("SELECT business_no FROM quotes WHERE business_no=? FOR UPDATE",String.class,no);
        if(found.isEmpty())throw new NotFoundException();return quoteRepo.findByBusinessNo(no).orElseThrow(NotFoundException::new);
    }
    private String lockOrder(String no){
        List<String> found=jdbc.queryForList("SELECT order_no FROM customer_orders WHERE COALESCE(business_no,order_no)=? FOR UPDATE",String.class,no);
        if(found.isEmpty())throw new NotFoundException();return found.getFirst();
    }
    private static void expected(Quote quote,String status){
        if(status==null||!quote.getStatus().name().equals(status))throw new OptimisticLockingFailureException("询价状态已变化，请重新读取后处理；当前输入已保留");
    }
    public record QuoteChange(String status,@NotBlank String expectedStatus,@Size(max=500) String reason){}
}
