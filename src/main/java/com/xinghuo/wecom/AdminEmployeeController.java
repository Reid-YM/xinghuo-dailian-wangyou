package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 星维网络服务工作室
 * 企业微信员工管理后台接口。
 *
 * ADMIN / OPERATOR 可读取通讯录。
 * 新增、调岗、离职停用仅允许 ADMIN。
 */
@RestController
@RequestMapping("/api/admin/employees")
public class AdminEmployeeController {

    private final WeComHrService hr;
    private final AuditService audits;

    public AdminEmployeeController(
            WeComHrService hr,
            AuditService audits) {

        this.hr = hr;
        this.audits = audits;
    }

    /**
     * 实时员工列表。
     */
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public WeComHrService.EmployeeListResponse employees(Authentication authentication) {
        WeComHrService.EmployeeListResponse response = hr.employees();
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return response;
        }
        List<WeComHrService.EmployeeItem> redacted = response.items().stream()
                .map(item -> new WeComHrService.EmployeeItem(
                        item.userId(), item.name(), item.position(), "", "", "",
                        item.status(), item.departmentIds(), item.departments()))
                .toList();
        return new WeComHrService.EmployeeListResponse(response.readConfigured(), false,
                response.total(), redacted);
    }

    /**
     * 实时企业微信部门结构。
     */
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/departments")
    public WeComHrService.DepartmentListResponse departments() {
        return hr.departments();
    }

    /**
     * 唯一正式155职务体系。
     */
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping("/positions")
    public JsonNode positions() {
        return hr.positions();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void create(@RequestBody WeComHrService.NewEmployeeRequest request,
                       Authentication authentication, HttpServletRequest servletRequest) {
        hr.createEmployee(request);
        audits.log("WECOM_EMPLOYEE_CREATE", "WECOM_EMPLOYEE", request.userId(),
                authentication.getName(), null,
                Map.of("departmentId", request.departmentId(), "position", request.position()),
                "企业微信新增成员", servletRequest);
    }

    @PostMapping("/{userId}/departure")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void departure(@PathVariable String userId,
                          Authentication authentication, HttpServletRequest servletRequest) {
        hr.disableEmployee(userId);
        audits.log("WECOM_EMPLOYEE_DISABLE", "WECOM_EMPLOYEE", userId,
                authentication.getName(), null, Map.of("enabled", false),
                "企业微信离职停用", servletRequest);
    }

    @PatchMapping("/{userId}/assignment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void transfer(@PathVariable String userId,
                         @RequestBody WeComHrService.AssignmentRequest request,
                         Authentication authentication, HttpServletRequest servletRequest) {
        hr.transferEmployee(userId, request);
        audits.log("WECOM_EMPLOYEE_TRANSFER", "WECOM_EMPLOYEE", userId,
                authentication.getName(), null,
                Map.of("departmentId", request.departmentId(), "position", request.position()),
                "企业微信调岗", servletRequest);
    }

}
