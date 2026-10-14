package com.xinghuo.controller;

import com.xinghuo.service.XingweiBusinessScopeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 星维 V2D：询价/订单新版只读列表与统一业务归属。 */
@RestController
@RequestMapping("/api/admin/xingwei/business")
public class XingweiBusinessScopeController {
    private final XingweiBusinessScopeService business;

    public XingweiBusinessScopeController(XingweiBusinessScopeService business) {
        this.business = business;
    }

    @GetMapping("/quotes")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'quote:view')")
    public Map<String, Object> quotes(Authentication authentication) {
        return business.quoteSnapshot(authentication);
    }

    @GetMapping("/orders")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public Map<String, Object> orders(Authentication authentication) {
        return business.orderSnapshot(authentication);
    }

    @GetMapping("/orders/exceptions")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public Map<String, Object> exceptionOrders(Authentication authentication) {
        return business.exceptionOrderSnapshot(authentication);
    }

    @GetMapping("/orders/after-sales")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public Map<String, Object> afterSalesOrders(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        return business.afterSalesOrderSnapshot(authentication, q, page, category);
    }

    @GetMapping("/ownership-candidates")
    @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication, 'quote:manage', 'order:assign')")
    public List<Map<String, Object>> ownershipCandidates(Authentication authentication) {
        return business.ownershipCandidates(authentication);
    }

    @PutMapping("/quotes/{businessNo}/ownership")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'quote:manage')")
    public Map<String, Object> assignQuote(
            @PathVariable String businessNo,
            @RequestBody OwnershipRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        return business.assignQuote(
                businessNo,
                body.ownerAdminUserId(),
                body.departmentCode(),
                authentication,
                request);
    }

    @PutMapping("/orders/{businessNo}/ownership")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:assign')")
    public Map<String, Object> assignOrder(
            @PathVariable String businessNo,
            @RequestBody OwnershipRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        return business.assignOrder(
                businessNo,
                body.ownerAdminUserId(),
                body.departmentCode(),
                authentication,
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        return Map.of(
                "ok", false,
                "error", ex.getMessage() == null ? "请求参数不合法" : ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> forbidden(AccessDeniedException ex) {
        return Map.of(
                "ok", false,
                "error", ex.getMessage() == null ? "超出当前数据范围" : ex.getMessage());
    }

    public record OwnershipRequest(Long ownerAdminUserId, String departmentCode) {}
}
