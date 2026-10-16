package com.xinghuo.service;

import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.XingweiCatalogRevisionService.Kind;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/** Restores legacy maintenance capabilities without exposing a legacy API bypass. */
@Service
public class XingweiBusinessMaintenanceService {
    private final JdbcTemplate jdbc;private final XingweiPermissionGuard guard;private final XingweiBusinessScopeService scope;private final XingweiCatalogRevisionService revisions;private final QuoteService quotes;private final OrderService orders;private final EntityManager entities;private final AuditService audit;
    public XingweiBusinessMaintenanceService(JdbcTemplate jdbc,XingweiPermissionGuard guard,XingweiBusinessScopeService scope,XingweiCatalogRevisionService revisions,QuoteService quotes,OrderService orders,EntityManager entities,AuditService audit){this.jdbc=jdbc;this.guard=guard;this.scope=scope;this.revisions=revisions;this.quotes=quotes;this.orders=orders;this.entities=entities;this.audit=audit;}
    private void require(Authentication actor,String permission){if(!guard.has(actor,permission))throw new AccessDeniedException("没有对应的业务维护权限");}
    private boolean order(String type){if("orders".equals(type))return true;if("quotes".equals(type))return false;throw new BadRequestException("业务类型无效");}
    public Map<String,Object> access(Authentication actor){return Map.of("canViewOrders",guard.has(actor,"order:view"),"canViewQuotes",guard.has(actor,"quote:view"),"canPublishCase",guard.has(actor,"order:manage"),"canDelete",guard.resolve(actor).allowAll());}
    @Transactional(readOnly=true) public Map<String,Object> snapshot(String type,String q,int page,Authentication actor){
        boolean order=order(type);require(actor,order?"order:view":"quote:view");if(page<0||page>10000)throw new BadRequestException("页码超出范围");String query=q==null?"":q.trim();if(query.length()>120)throw new BadRequestException("关键词不能超过120字");var clause=scope.queryScope(actor);var args=new ArrayList<Object>(clause.args());String no=order?"COALESCE(t.business_no,t.order_no)":"COALESCE(t.business_no,t.id)",key=order?"t.order_no":"t.id";
        String from=" FROM "+(order?"customer_orders":"quotes")+" t LEFT JOIN xw_business_ownership o ON o.business_no="+no+" LEFT JOIN xw_departments d ON d.id=o.department_id WHERE ("+clause.sql()+")";
        if(!query.isEmpty()){String pattern="%"+query.replace("!","!!").replace("%","!%").replace("_","!_")+"%";from+=" AND ("+no+" LIKE ? ESCAPE '!' OR t.status LIKE ? ESCAPE '!')";args.add(pattern);args.add(pattern);}
        Long total=jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());args.add(25);args.add(page*25);var keys=jdbc.queryForList("SELECT "+key+from+" ORDER BY t.created_at DESC LIMIT ? OFFSET ?",String.class,args.toArray());return Map.of("items",keys.stream().map(id->state(type,id)).toList(),"total",total==null?0:total,"page",page,"size",25);
    }
    private State state(String type,String key){boolean order=order(type);String sql=order?"SELECT order_no AS record_key,COALESCE(business_no,order_no) AS business_no,status,allow_public_case,public_summary,updated_at FROM customer_orders WHERE order_no=?":"SELECT id AS record_key,COALESCE(business_no,id) AS business_no,status,updated_at FROM quotes WHERE id=?";
        var rows=jdbc.query(sql,(rs,n)->{var row=new LinkedHashMap<String,Object>();row.put("recordKey",rs.getString("record_key"));row.put("businessNo",rs.getString("business_no"));row.put("status",rs.getString("status"));row.put("updatedAt",rs.getTimestamp("updated_at"));if(order){row.put("allowPublicCase",rs.getBoolean("allow_public_case"));row.put("publicSummary",rs.getString("public_summary"));}return row;},key);if(rows.isEmpty())throw new NotFoundException();return new State(rows.getFirst(),revisions.current(order?Kind.ORDER:Kind.QUOTE,key));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) public State publicCase(String key,PublicCaseInput body,String expected,Authentication actor,HttpServletRequest request){
        require(actor,"order:manage");var before=state("orders",key);String no=String.valueOf(before.data().get("businessNo"));if(!scope.canAccessBusiness(actor,no))throw new AccessDeniedException("订单不在可管理范围内");revisions.lock(Kind.ORDER,key,expected);if(body.enabled()&&!body.privacyConfirmed())throw new BadRequestException("公开前必须确认客户授权，并核对摘要没有个人信息");
        orders.setPublicCase(key,body.enabled(),body.summary(),actor.getName());entities.flush();audit.log("XW_ORDER_PUBLIC_CASE","ORDER",no,actor.getName(),Map.of("enabled",before.data().get("allowPublicCase")),Map.of("enabled",body.enabled(),"privacyConfirmed",body.privacyConfirmed()),body.reason(),request);return state("orders",key);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) public void delete(String type,String key,DeleteInput body,String expected,Authentication actor,HttpServletRequest request){
        boolean order=order(type);require(actor,order?"order:manage":"quote:manage");if(!guard.resolve(actor).allowAll())throw new AccessDeniedException("永久删除业务记录仅限恢复 ADMIN");var before=state(type,key);if(!Objects.equals(before.data().get("businessNo"),body.confirmation()))throw new BadRequestException("请准确输入业务编号确认删除");revisions.lock(order?Kind.ORDER:Kind.QUOTE,key,expected);if(order)orders.delete(key,actor.getName(),request);else quotes.delete(UUID.fromString(key),actor.getName(),request);entities.flush();
    }
    public record State(Map<String,Object> data,String revision){}
    public record PublicCaseInput(boolean enabled,@Size(max=240) String summary,boolean privacyConfirmed,@NotBlank @Size(max=500) String reason){}
    public record DeleteInput(@NotBlank @Size(max=80) String confirmation){}
}
