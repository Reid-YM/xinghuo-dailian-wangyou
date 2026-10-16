package com.xinghuo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.service.Masking;
import com.xinghuo.account.web.AccountAdminApiController;
import com.xinghuo.dto.ApiDtos;
import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.repository.QuoteRepository;
import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Native adapters reuse existing services; global datasets never inherit business-only scopes. */
@Service
public class XingweiOperationsService {
    private final JdbcTemplate jdbc; private final XingweiPermissionGuard guard;
    private final AccountAdminApiController account; private final AdminTestFeedbackService feedback;
    private final SlotService slots; private final ServiceCatalogService catalog; private final QuoteService quotes;
    private final QuoteRepository quoteRepo; private final XingweiBusinessScopeService business;
    private final UploadService uploads; private final AuditService audit; private final AccountSessionService sessions;
    private final EntityManager entities; private final ObjectMapper json;
    public XingweiOperationsService(JdbcTemplate jdbc,XingweiPermissionGuard guard,AccountAdminApiController account,AdminTestFeedbackService feedback,SlotService slots,ServiceCatalogService catalog,QuoteService quotes,QuoteRepository quoteRepo,XingweiBusinessScopeService business,UploadService uploads,AuditService audit,AccountSessionService sessions,EntityManager entities,ObjectMapper json){
        this.jdbc=jdbc;this.guard=guard;this.account=account;this.feedback=feedback;this.slots=slots;this.catalog=catalog;this.quotes=quotes;this.quoteRepo=quoteRepo;this.business=business;this.uploads=uploads;this.audit=audit;this.sessions=sessions;this.entities=entities;this.json=json;
    }
    public Map<String,Object> access(Authentication actor){
        var result=new LinkedHashMap<String,Object>();
        for(var entry:Map.of("members","customer","community","content","feedback","feedback","display","site").entrySet())result.put(entry.getKey(),Map.of("view",global(actor,entry.getValue()+":view"),"manage",global(actor,entry.getValue()+":manage")));
        result.put("crm",Map.of("view",guard.has(actor,"quote:view"),"manage",guard.has(actor,"quote:manage")));
        return Map.of("modules",result,"restoreAdmin",guard.resolve(actor).allowAll());
    }
    public boolean global(Authentication actor,String permission){var a=guard.resolve(actor);return !"DENIED".equals(a.mode())&&guard.has(actor,permission)&&(a.allowAll()||"ALL".equals(a.dataScope()));}
    private void requireGlobal(Authentication actor,String permission){if(!global(actor,permission))throw new AccessDeniedException("此全站模块需要对应权限与全部数据范围");}
    @Transactional(readOnly=true) public Page members(String q,String status,int page,Authentication actor){
        requireGlobal(actor,"customer:view");var clause=filter(q,status,List.of("public_user_id","display_name","email"));return queryPage("customer_accounts",clause,page,id->member(id),"created_at");
    }
    @Transactional(readOnly=true) public State member(long id,Authentication actor){requireGlobal(actor,"customer:view");return member(id);}
    private State member(long id){
        var rows=jdbc.query("SELECT id,public_user_id,email,email_verified,display_name,role,status,created_at,updated_at,last_login_at FROM customer_accounts WHERE id=?",(rs,n)->{var row=new LinkedHashMap<String,Object>();row.put("id",rs.getLong("id"));row.put("publicUserId",rs.getString("public_user_id"));row.put("email",rs.getString("email"));row.put("emailVerified",rs.getBoolean("email_verified"));row.put("displayName",rs.getString("display_name"));row.put("role",rs.getString("role"));row.put("status",rs.getString("status"));for(String key:List.of("created_at","updated_at","last_login_at"))row.put(switch(key){case "created_at"->"createdAt";case "updated_at"->"updatedAt";default->"lastLoginAt";},rs.getTimestamp(key)==null?null:rs.getTimestamp(key).toInstant());return new State(row,revision("customer_accounts",id,rs.getTimestamp("updated_at")));},id);
        if(rows.isEmpty())throw new NotFoundException();return rows.getFirst();
    }
    @Transactional public State createMember(AccountAdminApiController.UserCreateRequest body,Authentication actor,HttpServletRequest request){
        requireGlobal(actor,"customer:manage");elevatedRole(body.role(),actor);var result=account.createUser(body,actor);entities.flush();audit.log("XW_CUSTOMER_CREATE","CUSTOMER",String.valueOf(result.id()),actor.getName(),null,Map.of("role",result.role(),"status",result.status()),"新增网站会员",request);return member(result.id());
    }
    @Transactional public State updateMember(long id,AccountAdminApiController.UserUpdateRequest body,String expected,Authentication actor,HttpServletRequest request){
        requireGlobal(actor,"customer:manage");elevatedRole(body.role(),actor);checkMember(id,expected,actor);var before=member(id);var result=account.updateUser(id,body);if(!"ACTIVE".equals(result.status()))sessions.revokeAll(id);entities.flush();audit.log("XW_CUSTOMER_UPDATE","CUSTOMER",String.valueOf(id),actor.getName(),safeMember(before),Map.of("role",result.role(),"status",result.status()),"维护网站会员",request);return member(id);
    }
    @Transactional public State verifyMember(long id,AccountAdminApiController.EmailVerifiedRequest body,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"customer:manage");checkMember(id,expected,actor);account.setEmailVerified(id,body);entities.flush();audit.log("XW_CUSTOMER_VERIFY","CUSTOMER",String.valueOf(id),actor.getName(),null,Map.of("verified",body.verified()),"维护会员邮箱核验状态",request);return member(id);}
    @Transactional public State resetMemberPassword(long id,AccountAdminApiController.PasswordResetRequest body,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"customer:manage");checkMember(id,expected,actor);account.resetUserPassword(id,body);sessions.revokeAll(id);entities.flush();audit.log("XW_CUSTOMER_PASSWORD_RESET","CUSTOMER",String.valueOf(id),actor.getName(),null,Map.of("sessionsRevoked",true),"会员密码重置并撤销登录",request);return member(id);}
    @Transactional public void deleteMember(long id,String confirmation,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"customer:manage");if(!guard.resolve(actor).allowAll())throw new AccessDeniedException("彻底删除会员仅限恢复管理员");checkMember(id,expected,actor);var before=member(id);var row=map(before.data());if(!Objects.equals(row.get("publicUserId")==null?"ID-"+id:row.get("publicUserId"),confirmation))throw new BadRequestException("请准确输入会员公开编号确认删除");sessions.revokeAll(id);account.deleteUser(id);entities.flush();audit.log("XW_CUSTOMER_DELETE","CUSTOMER",String.valueOf(id),actor.getName(),safeMember(before),null,"按会员公开编号确认彻底删除",request);}
    @Transactional(readOnly=true) public List<Map<String,Object>> loginLogs(long id,Authentication actor){requireGlobal(actor,"customer:view");member(id);return account.loginLogs(id,50).stream().map(item->{var row=new LinkedHashMap<String,Object>();row.put("action",item.action());row.put("ipMasked",Masking.ip(item.ip()));row.put("createdAt",item.createdAt());return (Map<String,Object>)row;}).toList();}
    private void checkMember(long id,String expected,Authentication actor){check("customer_accounts","id",id,expected);elevatedRole(String.valueOf(map(member(id).data()).get("role")),actor);}
    private void elevatedRole(String role,Authentication actor){if(role!=null&&Set.of("ADMIN","OPERATOR").contains(role.toUpperCase(Locale.ROOT))&&!guard.resolve(actor).allowAll())throw new AccessDeniedException("特殊会员角色仅限恢复管理员调整");}
    private Map<String,Object> safeMember(State state){var row=map(state.data());return Map.of("role",row.get("role"),"status",row.get("status"));}

    @Transactional(readOnly=true) public Page comments(String status,int page,Authentication actor){requireGlobal(actor,"content:view");String value=enumValue(status,"PENDING",Set.of("PENDING","PUBLISHED","REJECTED","DELETED"));int p=page(page);var list=account.comments(value,p,25);return new Page(list.stream().map(item->state("user_comment",item.id(),item)).toList(),p,25,count("user_comment",new Clause("status=?",List.of(value))));}
    @Transactional public State reviewComment(long id,AccountAdminApiController.ReviewRequest body,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"content:manage");check("user_comment","id",id,expected);account.reviewComment(id,body,actor);entities.flush();audit.log("XW_COMMENT_REVIEW","COMMENT",String.valueOf(id),actor.getName(),null,Map.of("approved",body.approve()),"评论审核",request);return comment(id);}
    @Transactional public void deleteComment(long id,String note,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"content:manage");check("user_comment","id",id,expected);account.deleteComment(id,note,actor);entities.flush();audit.log("XW_COMMENT_DELETE","COMMENT",String.valueOf(id),actor.getName(),null,Map.of("deleted",true),"删除评论",request);}
    private State comment(long id){var data=jdbc.queryForMap("SELECT id,target_type,target_id,content,status,created_at FROM user_comment WHERE id=?",id);var clean=new LinkedHashMap<String,Object>();data.forEach((key,value)->clean.put(camel(key),value));return state("user_comment",id,clean);}
    @Transactional(readOnly=true) public Page submissions(String status,int page,Authentication actor){requireGlobal(actor,"content:view");String value=enumValue(status,"SUBMITTED",Set.of("SUBMITTED","IN_REVIEW","APPROVED","REJECTED","PUBLISHED","WITHDRAWN"));int p=page(page);var list=account.submissions(value,p,25);return new Page(list.stream().map(item->state("user_submission",item.id(),item)).toList(),p,25,count("user_submission",new Clause("status=?",List.of(value))));}
    @Transactional(readOnly=true) public State submission(long id,Authentication actor){requireGlobal(actor,"content:view");return state("user_submission",id,account.submission(id));}
    @Transactional public State reviewSubmission(long id,AccountAdminApiController.SubmissionReviewRequest body,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"content:manage");check("user_submission","id",id,expected);var result=account.reviewSubmission(id,body,actor);entities.flush();audit.log("XW_SUBMISSION_REVIEW","SUBMISSION",String.valueOf(id),actor.getName(),null,Map.of("status",result.status()),"投稿审核",request);return state("user_submission",id,result);}
    @Transactional public State publishSubmission(long id,AccountAdminApiController.PublishRequest body,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"content:manage");check("user_submission","id",id,expected);Long found=jdbc.queryForObject("SELECT COUNT(*) FROM game_guides WHERE slug=? AND status='PUBLISHED'",Long.class,body.slug());if(found==null||found==0)throw new BadRequestException("请先在攻略工作台发布对应内容，再关联投稿");var result=account.publishSubmission(id,body,actor);entities.flush();audit.log("XW_SUBMISSION_PUBLISH","SUBMISSION",String.valueOf(id),actor.getName(),null,Map.of("published",true),"关联已发布攻略",request);return state("user_submission",id,result);}

    @Transactional(readOnly=true) public Page feedback(String q,String status,int page,Authentication actor){requireGlobal(actor,"feedback:view");return queryPage("admin_test_feedback",filter(q,status,List.of("title","content","page_path")),page,id->state("admin_test_feedback",id,feedback.get(id)),"updated_at");}
    @Transactional public State createFeedback(ApiDtos.TestFeedbackCreateRequest body,Authentication actor,HttpServletRequest request){requireGlobal(actor,"feedback:manage");var result=feedback.create(body,actor.getName(),request);entities.flush();return state("admin_test_feedback",result.id(),result);}
    @Transactional public State updateFeedback(long id,ApiDtos.TestFeedbackUpdateRequest body,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"feedback:manage");check("admin_test_feedback","id",id,expected);var result=feedback.update(id,body,actor.getName(),request);entities.flush();return state("admin_test_feedback",id,result);}
    @Transactional public State feedbackImage(long id,MultipartFile file,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"feedback:manage");check("admin_test_feedback","id",id,expected);var asset=uploads.storeImage(file,UploadAsset.Kind.TEST_FEEDBACK_ATTACHMENT);uploads.bind(asset.getId(),"TEST_FEEDBACK",String.valueOf(id));var result=feedback.attach(id,asset.getId(),actor.getName(),request);entities.flush();return state("admin_test_feedback",id,result);}
    @Transactional(readOnly=true) public ResponseEntity<Resource> feedbackImage(long id,Authentication actor){requireGlobal(actor,"feedback:view");var item=feedback.get(id);if(item.attachmentAssetId()==null)throw new NotFoundException();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(uploads.mediaType(item.attachmentAssetId())).body(uploads.load(item.attachmentAssetId(),"TEST_FEEDBACK",String.valueOf(id)));}
    @Transactional(readOnly=true) public List<State> display(Authentication actor){requireGlobal(actor,"site:view");return slots.list().stream().map(item->stateBy("hot_slots","slot_no",item.slotNo(),item)).toList();}
    @Transactional(readOnly=true) public List<Map<String,Object>> displayServices(Authentication actor){requireGlobal(actor,"site:view");return catalog.adminList().stream().map(item->Map.<String,Object>of("id",item.id(),"name",item.name(),"status",item.status(),"gameCode",item.gameCode())).toList();}
    @Transactional public State updateDisplay(ApiDtos.SlotUpdate body,String expected,Authentication actor,HttpServletRequest request){requireGlobal(actor,"site:manage");check("hot_slots","slot_no",body.slotNo(),expected);var result=slots.update(body,actor.getName(),request);entities.flush();return stateBy("hot_slots","slot_no",body.slotNo(),result);}

    @Transactional(readOnly=true) public Page leads(String q,String status,int page,Authentication actor){if(!guard.has(actor,"quote:view"))throw new AccessDeniedException("没有线索查看权限");var scope=business.queryScope(actor);var filter=filter(q,status,List.of("q.business_no","q.game","q.demand","q.customer_contact"),"q.status");var args=new ArrayList<Object>(scope.args());args.addAll(filter.args());String where="("+scope.sql()+") AND ("+filter.sql()+")";String from=" FROM quotes q LEFT JOIN xw_business_ownership o ON o.business_no=q.business_no LEFT JOIN xw_departments d ON d.id=o.department_id WHERE "+where;int p=page(page);Long total=jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,args.toArray());args.add(25);args.add(p*25);var items=jdbc.queryForList("SELECT q.id"+from+" ORDER BY q.created_at DESC LIMIT ? OFFSET ?",args.toArray()).stream().map(row->leadById(row.get("id"))).toList();return new Page(items,p,25,total==null?0:total);}
    @Transactional(readOnly=true) public State lead(String businessNo,Authentication actor){if(!guard.has(actor,"quote:view")||!business.canAccessBusiness(actor,businessNo))throw new AccessDeniedException("线索不在可见范围内");return leadById(jdbc.queryForObject("SELECT id FROM quotes WHERE business_no=?",String.class,businessNo));}
    private State leadById(Object id){var row=jdbc.queryForMap("SELECT id,business_no,game,demand,current_progress,customer_contact,contact_method,status,lead_score,lead_intent_level,next_action,follow_up_note,ai_suggestion,last_contacted_at,first_touch_source,utm_source,created_at,updated_at FROM quotes WHERE id=?",id);var data=new LinkedHashMap<String,Object>();row.forEach((key,value)->data.put(camel(key),value));return stateBy("quotes","id",id,data);}
    @Transactional public State updateLead(String no,QuoteLeadDtos.QuoteCrmUpdateRequest body,String expected,boolean suggestion,Authentication actor,HttpServletRequest request){if(!guard.has(actor,"quote:manage")||!business.canAccessBusiness(actor,no))throw new AccessDeniedException("线索不在可管理范围内");String id=jdbc.queryForObject("SELECT id FROM quotes WHERE business_no=?",String.class,no);check("quotes","id",id,expected);if(suggestion)quotes.generateLeadSuggestion(UUID.fromString(id),actor.getName(),request);else quotes.updateCrm(UUID.fromString(id),body,actor.getName(),request);quoteRepo.flush();return leadById(id);}

    private Page queryPage(String table,Clause clause,int page,java.util.function.LongFunction<State> mapper,String order){int p=page(page);var args=new ArrayList<Object>(clause.args());long total=count(table,clause);args.add(25);args.add(p*25);var items=jdbc.queryForList("SELECT id FROM "+table+" WHERE "+clause.sql()+" ORDER BY "+order+" DESC,id DESC LIMIT ? OFFSET ?",Long.class,args.toArray()).stream().map(mapper::apply).toList();return new Page(items,p,25,total);}
    private long count(String table,Clause clause){Long count=jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+clause.sql(),Long.class,clause.args().toArray());return count==null?0:count;}
    private Clause filter(String q,String status,List<String> columns){return filter(q,status,columns,"status");}
    private Clause filter(String q,String status,List<String> columns,String statusColumn){var sql=new StringBuilder("1=1");var args=new ArrayList<Object>();if(q!=null&&!q.isBlank()){String value=q.trim();if(value.length()>120)throw new BadRequestException("关键词不能超过120字");String pattern="%"+value.replace("!","!!").replace("%","!%").replace("_","!_")+"%";sql.append(" AND (");sql.append(String.join(" OR ",columns.stream().map(name->name+" LIKE ? ESCAPE '!'").toList()));sql.append(")");columns.forEach(c->args.add(pattern));}if(status!=null&&!status.isBlank()){sql.append(" AND ").append(statusColumn).append("=?");args.add(status.trim().toUpperCase(Locale.ROOT));}return new Clause(sql.toString(),args);}
    private int page(int page){if(page<0||page>10000)throw new BadRequestException("页码超出范围");return page;}
    private String enumValue(String value,String fallback,Set<String> allowed){String result=value==null||value.isBlank()?fallback:value.trim().toUpperCase(Locale.ROOT);if(!allowed.contains(result))throw new BadRequestException("不支持的状态");return result;}
    private State state(String table,Object id,Object data){return stateBy(table,"id",id,data);}
    private State stateBy(String table,String key,Object id,Object data){return new State(data,readRevision(table,key,id,false));}
    private void check(String table,String key,Object id,String expected){String actual=readRevision(table,key,id,true);if(expected==null||(!expected.equals(actual)&&!expected.equals("\""+actual+"\"")))throw new OptimisticLockingFailureException("记录已变化，请重新读取并核对；当前输入尚未保存");}
    private String readRevision(String table,String key,Object id,boolean lock){var rows=jdbc.query("SELECT updated_at FROM "+table+" WHERE "+key+"=?"+(lock?" FOR UPDATE":""),(rs,n)->revision(table,id,rs.getTimestamp(1)),id);if(rows.isEmpty())throw new NotFoundException();return rows.getFirst();}
    private static String revision(String table,Object id,Object updated){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((table+":"+id+":"+updated).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    @SuppressWarnings("unchecked") private Map<String,Object> map(Object value){return json.convertValue(value,Map.class);}
    private static String camel(String raw){var out=new StringBuilder();boolean upper=false;for(char c:raw.toLowerCase(Locale.ROOT).toCharArray()){if(c=='_'){upper=true;}else{out.append(upper?Character.toUpperCase(c):c);upper=false;}}return out.toString();}
    public record State(Object data,String revision){} public record Page(List<State> items,int page,int size,long total){} private record Clause(String sql,List<Object> args){}
}
