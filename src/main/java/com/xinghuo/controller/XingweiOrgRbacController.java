package com.xinghuo.controller;

import com.xinghuo.service.AuditService;
import com.xinghuo.service.XingweiOrgRbacService;
import jakarta.servlet.http.HttpServletRequest;
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

    public XingweiOrgRbacController(XingweiOrgRbacService org, AuditService audits) {
        this.org = org;
        this.audits = audits;
    }

    @GetMapping("/snapshot")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'org:view')")
    public Map<String, Object> snapshot() {
        return org.snapshot();
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
