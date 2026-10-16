package com.xinghuo.service;

import com.xinghuo.account.service.PasswordPolicy;
import com.xinghuo.exception.BadRequestException;
import com.xinghuo.exception.NotFoundException;
import com.xinghuo.security.XingweiPermissionGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import java.sql.Statement;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Employee login lifecycle, separate from public website members and external WeCom writes. */
@Service
public class XingweiAdminAccountService {
    private final JdbcTemplate jdbc;
    private final XingweiPermissionGuard guard;
    private final XingweiOrgRbacService organization;
    private final PasswordEncoder passwords;
    private final AuditService audits;
    public XingweiAdminAccountService(JdbcTemplate jdbc,XingweiPermissionGuard guard,XingweiOrgRbacService organization,PasswordEncoder passwords,AuditService audits){this.jdbc=jdbc;this.guard=guard;this.organization=organization;this.passwords=passwords;this.audits=audits;}

    private boolean allowed(Authentication actor,String permission){var access=guard.resolve(actor);return !"DENIED".equals(access.mode())&&guard.has(actor,permission)&&(access.allowAll()||"ALL".equals(access.dataScope()));}
    private void require(Authentication actor,String permission){if(!allowed(actor,permission))throw new AccessDeniedException("后台登录账号管理需要对应员工权限及全部数据范围");}
    @Transactional(readOnly=true) public Map<String,Object> snapshot(Authentication actor,String q){
        require(actor,"employee:view");String query=q==null?"":q.trim().toLowerCase(Locale.ROOT);if(query.length()>120)throw new BadRequestException("关键词不能超过120字");
        var items=organization.employees().stream().filter(row->query.isEmpty()||String.valueOf(row.get("username")).toLowerCase(Locale.ROOT).contains(query)||String.valueOf(row.get("displayName")).toLowerCase(Locale.ROOT).contains(query)).map(row->{long id=((Number)row.get("id")).longValue();var clean=new LinkedHashMap<String,Object>();for(String field:List.of("id","username","legacyRole","loginEnabled","displayName","departmentCode","departmentName","roleCodes"))clean.put(field,row.get(field));clean.put("authVersion",jdbc.queryForObject("SELECT auth_version FROM admin_users WHERE id=?",Long.class,id));return clean;}).toList();
        boolean manage=allowed(actor,"employee:manage");
        return Map.of("items",items,"canManage",manage,"canCreate",manage&&guard.has(actor,"rbac:manage"),"restoreAdmin",guard.resolve(actor).allowAll(),"roles",manage?organization.roles():List.of(),"departments",manage?organization.departments():List.of());
    }
    @Transactional public void create(CreateInput body,Authentication actor,HttpServletRequest request){
        require(actor,"employee:manage");if(!guard.has(actor,"rbac:manage"))throw new AccessDeniedException("创建后台账号还需要角色分配权限");
        String username=body.username().trim();if(!username.matches("[A-Za-z0-9_.-]{3,64}"))throw new BadRequestException("登录名需为3至64位字母、数字、点、下划线或短横线");
        String role=role(body.legacyRole());if("ADMIN".equals(role)&&!guard.resolve(actor).allowAll())throw new AccessDeniedException("恢复管理员只能由现有恢复管理员创建");
        PasswordPolicy.validate(body.password(),username,body.displayName());
        if(body.departmentCode()!=null&&!body.departmentCode().isBlank()){Long found=jdbc.queryForObject("SELECT COUNT(*) FROM xw_departments WHERE code=? AND enabled=TRUE",Long.class,body.departmentCode().trim());if(found==null||found!=1)throw new BadRequestException("所属中心不存在或未启用");}
        List<String> roleCodes=body.roleCodes()==null?List.of():body.roleCodes().stream().map(String::trim).distinct().toList();
        if("OPERATOR".equals(role)&&roleCodes.isEmpty())throw new BadRequestException("员工账号必须明确分配至少一个启用角色");
        for(String code:roleCodes){Long count=jdbc.queryForObject("SELECT COUNT(*) FROM xw_rbac_roles WHERE code=? AND enabled=TRUE",Long.class,code);if(count==null||count!=1)throw new BadRequestException("所选星维角色不存在或未启用");}
        var keys=new GeneratedKeyHolder();String hash=passwords.encode(body.password());
        try{jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO admin_users(username,password_hash,role,enabled,auth_version,created_at,updated_at) VALUES(?,?,?,FALSE,0,NOW(6),NOW(6))",Statement.RETURN_GENERATED_KEYS);statement.setString(1,username);statement.setString(2,hash);statement.setString(3,role);return statement;},keys);}catch(DuplicateKeyException duplicate){throw new BadRequestException("登录名已存在");}
        long id=Objects.requireNonNull(keys.getKey()).longValue();
        organization.updateEmployeeProfile(id,new XingweiOrgRbacService.ProfileInput(null,body.displayName(),body.departmentCode(),null,"ACTIVE",null));
        if(!guard.canAssignEmployeeRoles(actor,id,roleCodes))throw new AccessDeniedException("不能为新账号分配这些角色");
        organization.replaceEmployeeRoles(id,roleCodes);
        jdbc.update("UPDATE admin_users SET enabled=TRUE,updated_at=NOW(6) WHERE id=?",id);
        audits.log("XW_ADMIN_ACCOUNT_CREATE","ADMIN_USER",String.valueOf(id),actor.getName(),null,Map.of("role",role,"roleCodes",roleCodes,"enabled",true),"创建后台登录账号并明确分配角色",request);
    }
    @Transactional public void update(long id,UpdateInput body,Authentication actor,HttpServletRequest request){
        require(actor,"employee:manage");
        // Lock every login identity in deterministic order so two simultaneous administrator
        // downgrades cannot both observe a different remaining administrator.
        var identities=jdbc.query("SELECT id,role,enabled FROM admin_users ORDER BY id FOR UPDATE",(rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"role",rs.getString("role"),"enabled",rs.getBoolean("enabled")));
        var target=locked(id,body.authVersion());String oldRole=String.valueOf(target.get("role")),newRole=role(body.legacyRole());
        protect(actor,target,newRole);
        if(actor.getName().equals(target.get("username"))&&(!body.enabled()||!oldRole.equals(newRole)))throw new BadRequestException("不能在此停用自己或改变自己的登录角色");
        boolean wasEnabled=Boolean.TRUE.equals(target.get("enabled"))||Objects.equals(target.get("enabled"),1);
        if(wasEnabled&&"ADMIN".equals(oldRole)&&(!body.enabled()||!"ADMIN".equals(newRole))){long remaining=identities.stream().filter(row->((Number)row.get("id")).longValue()!=id&&"ADMIN".equals(row.get("role"))&&Boolean.TRUE.equals(row.get("enabled"))).count();if(remaining==0)throw new BadRequestException("必须保留至少一个已启用的恢复管理员");}
        if(body.enabled()&&"OPERATOR".equals(newRole)){
            Long assigned=jdbc.queryForObject("SELECT COUNT(*) FROM xw_admin_roles ar JOIN xw_rbac_roles r ON r.id=ar.role_id AND r.enabled=TRUE JOIN xw_admin_profiles p ON p.admin_user_id=ar.admin_user_id AND p.status='ACTIVE' WHERE ar.admin_user_id=?",Long.class,id);
            if(assigned==null||assigned==0)throw new BadRequestException("启用员工账号前，请先配置在岗档案和启用的星维角色");
        }
        jdbc.update("UPDATE admin_users SET role=?,enabled=?,auth_version=auth_version+1,updated_at=NOW(6) WHERE id=?",newRole,body.enabled(),id);
        audits.log("XW_ADMIN_ACCOUNT_UPDATE","ADMIN_USER",String.valueOf(id),actor.getName(),Map.of("role",oldRole,"enabled",wasEnabled),Map.of("role",newRole,"enabled",body.enabled(),"sessionsRevoked",true),"维护后台登录状态并撤销旧会话",request);
    }
    @Transactional public void password(long id,PasswordInput body,Authentication actor,HttpServletRequest request){
        require(actor,"employee:manage");var target=locked(id,body.authVersion());protect(actor,target,String.valueOf(target.get("role")));
        PasswordPolicy.validate(body.newPassword(),String.valueOf(target.get("username")));
        jdbc.update("UPDATE admin_users SET password_hash=?,auth_version=auth_version+1,updated_at=NOW(6) WHERE id=?",passwords.encode(body.newPassword()),id);
        audits.log("XW_ADMIN_ACCOUNT_PASSWORD","ADMIN_USER",String.valueOf(id),actor.getName(),null,Map.of("sessionsRevoked",true),"重置后台密码并撤销旧会话",request);
    }
    private Map<String,Object> locked(long id,long expected){var rows=jdbc.query("SELECT id,username,role,enabled,auth_version FROM admin_users WHERE id=? FOR UPDATE",(rs,n)->Map.<String,Object>of("id",rs.getLong("id"),"username",rs.getString("username"),"role",rs.getString("role"),"enabled",rs.getBoolean("enabled"),"authVersion",rs.getLong("auth_version")),id);if(rows.isEmpty())throw new NotFoundException();var row=rows.getFirst();if(((Number)row.get("authVersion")).longValue()!=expected)throw new OptimisticLockingFailureException("账号已变化，请重新读取并核对；当前输入尚未保存");return row;}
    private void protect(Authentication actor,Map<String,Object> target,String newRole){if(("ADMIN".equals(target.get("role"))||"ADMIN".equals(newRole))&&!guard.resolve(actor).allowAll())throw new AccessDeniedException("恢复管理员账号仅允许现有恢复管理员维护");}
    private String role(String raw){String role=raw==null?"OPERATOR":raw.trim().toUpperCase(Locale.ROOT);if(!Set.of("ADMIN","OPERATOR").contains(role))throw new BadRequestException("登录角色仅支持 ADMIN / OPERATOR");return role;}
    public record CreateInput(@NotBlank @Size(max=64) String username,@NotBlank @Size(max=80) String displayName,@Size(max=16) String departmentCode,@NotBlank @Size(max=72) String password,@NotBlank String legacyRole,@Size(max=16) List<@NotBlank @Size(max=48) String> roleCodes){@Override public String toString(){return "CreateInput[credentials redacted]";}}
    public record UpdateInput(@NotBlank String legacyRole,boolean enabled,@Min(0) long authVersion){}
    public record PasswordInput(@NotBlank @Size(max=72) String newPassword,@Min(0) long authVersion){@Override public String toString(){return "PasswordInput[credentials redacted]";}}
}
