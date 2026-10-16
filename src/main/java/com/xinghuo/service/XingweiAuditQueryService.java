package com.xinghuo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.security.XingweiPermissionGuard.AccessSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Scoped audit metadata and deliberately restricted change summaries, never raw JSON or credentials. */
@Service
@Transactional(readOnly=true)
public class XingweiAuditQueryService {
    private static final int PAGE_SIZE=50;
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard guard;
    private final ObjectMapper mapper;
    public XingweiAuditQueryService(JdbcTemplate jdbc,XingweiPermissionGuard guard,ObjectMapper mapper) {
        this.jdbc=jdbc; this.guard=guard; this.mapper=mapper;
    }
    public Map<String,Object> list(Authentication actor,String q,String action,LocalDate from,LocalDate to,int page) {
        if(page<0 || page>10000) throw new IllegalArgumentException("页码超出范围");
        q=clean(q,80); action=clean(action,64);
        if(from!=null && to!=null && from.isAfter(to)) throw new IllegalArgumentException("开始日期不能晚于结束日期");
        AccessSnapshot access=require(actor);
        Clause scope=scope(actor,access);
        String where=scope.sql();
        List<Object> args=new ArrayList<>(scope.args());
        if(!q.isEmpty()) {
            where+=" AND (a.target_id LIKE ? ESCAPE '!' OR a.operator_name LIKE ? ESCAPE '!' OR a.request_id LIKE ? ESCAPE '!')";
            String term="%"+q.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            args.add(term); args.add(term); args.add(term);
        }
        if(!action.isEmpty()) { where+=" AND a.action=?"; args.add(action); }
        // Business-day filters use Beijing time explicitly, independent of the server timezone.
        if(from!=null) { where+=" AND a.created_at>=?"; args.add(java.sql.Timestamp.from(from.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant())); }
        if(to!=null) { where+=" AND a.created_at<?"; args.add(java.sql.Timestamp.from(to.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant())); }
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs a WHERE "+where,Long.class,args.toArray());
        List<Object> listArgs=new ArrayList<>(args); listArgs.add(PAGE_SIZE); listArgs.add(page*PAGE_SIZE);
        List<AuditItem> items=jdbc.query("SELECT a.* FROM audit_logs a WHERE "+where+" ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?",this::row,listArgs.toArray());
        List<String> actions=jdbc.query("SELECT DISTINCT a.action FROM audit_logs a WHERE "+scope.sql()+" ORDER BY a.action LIMIT 200",(rs,n)->rs.getString(1),scope.args().toArray());
        return Map.of("items",items,"totalCount",total,"page",page,"pageSize",PAGE_SIZE,"actions",actions,"dataScope",access.dataScope());
    }
    public Map<String,Object> detail(long id,Authentication actor) {
        Clause scope=scope(actor,require(actor));
        List<Object> args=new ArrayList<>(scope.args()); args.add(id);
        List<Map<String,Object>> rows=jdbc.query("SELECT a.* FROM audit_logs a WHERE "+scope.sql()+" AND a.id=?",(rs,n)->Map.<String,Object>of(
                "entry",row(rs,n),"changes",changes(rs.getString("before_json"),rs.getString("after_json"))),args.toArray());
        if(rows.isEmpty()) throw new AccessDeniedException("记录不存在或超出当前数据范围");
        return rows.getFirst();
    }
    private AuditItem row(ResultSet rs,int n)throws SQLException {
        return new AuditItem(rs.getLong("id"),rs.getString("action"),rs.getString("target_type"),rs.getString("target_id"),
                rs.getString("operator_name"),rs.getString("reason"),rs.getString("request_id"),rs.getTimestamp("created_at").toInstant().toString());
    }
    private AccessSnapshot require(Authentication actor) {
        AccessSnapshot a=guard.resolve(actor);
        if("DENIED".equals(a.mode()) || !guard.has(actor,"audit:view")) throw new AccessDeniedException("没有审计查看权限");
        return a;
    }
    private Clause scope(Authentication actor,AccessSnapshot access) {
        if(access.allowAll() || "ALL".equals(access.dataScope())) return new Clause("1=1",List.of());
        if(access.adminUserId()==null) return new Clause("1=0",List.of());
        if("DEPARTMENT".equals(access.dataScope()) && access.departmentCode()!=null)
            return new Clause("(a.operator_name=? OR EXISTS (SELECT 1 FROM admin_users u JOIN xw_admin_profiles p ON p.admin_user_id=u.id JOIN xw_departments d ON d.id=p.department_id WHERE u.username=a.operator_name AND d.code=?))",List.of(actor.getName(),access.departmentCode()));
        if("SELF".equals(access.dataScope()) || "DEPARTMENT".equals(access.dataScope())) return new Clause("a.operator_name=?",List.of(actor.getName()));
        return new Clause("1=0",List.of());
    }
    private List<Change> changes(String before,String after) {
        try {
            JsonNode left=before==null?null:mapper.readTree(before);
            JsonNode right=after==null?null:mapper.readTree(after);
            if((left!=null&&!left.isObject()) || (right!=null&&!right.isObject()))
                return List.of(new Change("记录内容","内容已隐藏","内容已隐藏"));
            Set<String> keys=new LinkedHashSet<>();
            if(left!=null) left.fieldNames().forEachRemaining(keys::add);
            if(right!=null) right.fieldNames().forEachRemaining(keys::add);
            List<Change> out=new ArrayList<>();
            for(String key:keys) {
                if(out.size()>=100) { out.add(new Change("其余字段","已省略","已省略")); break; }
                JsonNode l=left==null?null:left.get(key), r=right==null?null:right.get(key);
                if(java.util.Objects.equals(l,r)) continue;
                out.add(new Change(key.matches("[A-Za-z0-9_]{1,64}")?key:"其他字段",safe(key,l),safe(key,r)));
            }
            return out;
        } catch(Exception ignored) { return List.of(new Change("记录内容","无法解析，内容已隐藏","无法解析，内容已隐藏")); }
    }
    private String safe(String key,JsonNode value) {
        if(value==null||value.isNull()) return "未设置";
        if(Set.of("version","progressPercent","ownerAdminUserId","handledByAdminUserId").contains(key)&&value.isNumber()) return value.asText();
        if(Set.of("enabled","visible","published","isPrimary").contains(key)&&value.isBoolean()) return value.asText();
        if(Set.of("status","serviceState","dataScope").contains(key)&&value.isTextual()&&Set.of("TODO","IN_PROGRESS","DONE","CANCELLED","UNKNOWN","AVAILABLE","PAUSED","OPEN","RESOLVED","ALL","DEPARTMENT","SELF","WAITING_CONFIRMATION","COMPLETED","EXCEPTION","DRAFT","PUBLISHED","ARCHIVED","ACTIVE","INACTIVE").contains(value.asText())) return value.asText();
        return "内容已隐藏";
    }
    private static String clean(String value,int max) {
        value=value==null?"":value.trim();
        if(value.length()>max) throw new IllegalArgumentException("筛选内容过长");
        return value;
    }
    private record Clause(String sql,List<Object> args) {}
    public record AuditItem(long id,String action,String targetType,String targetId,String operatorName,String reason,String requestId,String createdAt) {}
    public record Change(String field,String before,String after) {}
}
