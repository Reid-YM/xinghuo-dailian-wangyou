package com.xinghuo.controller;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.AuditService;
import com.xinghuo.service.XingweiOrgRbacService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/xingwei/org")
public class XingweiOrgRbacController {
    private final XingweiOrgRbacService org;
    private final AuditService audits;
    private final XingweiPermissionGuard permissions;

    public XingweiOrgRbacController(
            XingweiOrgRbacService org,
            AuditService audits,
            XingweiPermissionGuard permissions) {
        this.org = org;
        this.audits = audits;
        this.permissions = permissions;
    }

    @GetMapping("/snapshot")
    @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication, 'org:view', 'employee:view', 'rbac:view')")
    public Map<String, Object> snapshot(Authentication authentication) {
        Map<String, Object> source = org.snapshot();
        boolean orgView = permissions.has(authentication, "org:view");
        boolean employeeView = permissions.has(authentication, "employee:view");
        boolean rbacView = permissions.has(authentication, "rbac:view");
        boolean organizationCatalogView = orgView || employeeView;

        Map<String, Object> result = new LinkedHashMap<>(source);
        result.put("shadowMode", false);
        result.put("enforcement", "XINGWEI_V2B_HYBRID");
        result.put("scope", "XINGWEI_ONLY");
        result.put("departments", organizationCatalogView ? source.get("departments") : List.of());
        result.put("positions", organizationCatalogView ? source.get("positions") : List.of());
        result.put("employees", employeeView ? source.get("employees") : List.of());
        result.put("roles", rbacView ? source.get("roles") : List.of());
        result.put("permissions", rbacView ? source.get("permissions") : List.of());

        Map<String, Object> sourceCounts = castMap(source.get("counts"));
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("departments", organizationCatalogView ? sourceCounts.getOrDefault("departments", 0) : 0);
        counts.put("positions", organizationCatalogView ? sourceCounts.getOrDefault("positions", 0) : 0);
        counts.put("employees", employeeView ? sourceCounts.getOrDefault("employees", 0) : 0);
        counts.put("assignedEmployees", employeeView ? sourceCounts.getOrDefault("assignedEmployees", 0) : 0);
        counts.put("roles", rbacView ? sourceCounts.getOrDefault("roles", 0) : 0);
        counts.put("permissions", rbacView ? sourceCounts.getOrDefault("permissions", 0) : 0);
        result.put("counts", counts);
        result.put("visibleSections", Map.of(
                "organization", organizationCatalogView,
                "employees", employeeView,
                "rbac", rbacView));
        return result;
    }

    @PutMapping("/employees/{adminUserId}/profile")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'employee:manage')")
    public Map<String, Object> updateEmployeeProfile(
            @PathVariable long adminUserId,
            @RequestBody EmployeeProfileRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> before = org.employee(adminUserId);
        Map<String, Object> after = org.updateEmployeeProfile(
                adminUserId,
                new XingweiOrgRbacService.ProfileInput(
                        body.employeeNo(),
                        body.displayName(),
                        body.departmentCode(),
                        body.positionCode(),
                        body.status(),
                        body.notes()));
        audits.log(
                "XW_EMPLOYEE_PROFILE_UPDATE",
                "XW_ADMIN_PROFILE",
                String.valueOf(adminUserId),
                operator(authentication),
                before,
                after,
                "星维 V2B 员工档案配置",
                request);
        return after;
    }

    @PutMapping("/employees/{adminUserId}/roles")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'rbac:manage')")
    public Map<String, Object> replaceEmployeeRoles(
            @PathVariable long adminUserId,
            @RequestBody EmployeeRolesRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> before = org.employee(adminUserId);
        Map<String, Object> after = org.replaceEmployeeRoles(adminUserId, body.roleCodes());
        audits.log(
                "XW_EMPLOYEE_ROLES_UPDATE",
                "XW_ADMIN_ROLE",
                String.valueOf(adminUserId),
                operator(authentication),
                before,
                after,
                "星维 V2B 角色配置",
                request);
        return after;
    }

    @PutMapping("/roles/{roleCode}/permissions")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'rbac:manage')")
    public Map<String, Object> replaceRolePermissions(
            @PathVariable String roleCode,
            @RequestBody RolePermissionsRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> before = org.role(roleCode);
        Map<String, Object> after = org.replaceRolePermissions(roleCode, body.permissionCodes());
        audits.log(
                "XW_ROLE_PERMISSIONS_UPDATE",
                "XW_RBAC_ROLE",
                roleCode,
                operator(authentication),
                before,
                after,
                "星维 V2B 权限配置",
                request);
        return after;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        return Map.of(
                "ok", false,
                "error", ex.getMessage() == null ? "请求参数不合法" : ex.getMessage());
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> missingReference() {
        return Map.of(
                "ok", false,
                "error", "所选账号、中心、岗位或角色不存在或已停用");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private static String operator(Authentication authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }

    public record EmployeeProfileRequest(
            String employeeNo,
            String displayName,
            String departmentCode,
            String positionCode,
            String status,
            String notes) {}

    public record EmployeeRolesRequest(List<String> roleCodes) {}

    public record RolePermissionsRequest(List<String> permissionCodes) {}
}
