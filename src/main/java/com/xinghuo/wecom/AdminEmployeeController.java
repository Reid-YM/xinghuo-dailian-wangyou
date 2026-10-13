package com.xinghuo.wecom;

import com.fasterxml.jackson.databind.JsonNode;
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

    public AdminEmployeeController(
            WeComHrService hr) {

        this.hr = hr;
    }

    /**
     * 实时员工列表。
     */
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @GetMapping
    public WeComHrService.EmployeeListResponse employees() {
        return hr.employees();
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
    public void create(@RequestBody WeComHrService.NewEmployeeRequest request) {
        hr.createEmployee(request);
    }

    @PostMapping("/{userId}/departure")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void departure(@PathVariable String userId) {
        hr.disableEmployee(userId);
    }

    @PatchMapping("/{userId}/assignment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void transfer(@PathVariable String userId,
                         @RequestBody WeComHrService.AssignmentRequest request) {
        hr.transferEmployee(userId, request);
    }

}
