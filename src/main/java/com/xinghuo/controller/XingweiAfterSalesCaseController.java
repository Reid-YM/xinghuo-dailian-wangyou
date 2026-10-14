package com.xinghuo.controller;

import com.xinghuo.service.XingweiAfterSalesCaseService;
import com.xinghuo.service.XingweiAfterSalesCaseService.CaseRecord;
import com.xinghuo.service.XingweiAfterSalesCaseService.CaseHistory;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Scoped staff handling of customer after-sales records. */
@RestController
@RequestMapping("/api/admin/xingwei/business/orders/{businessNo}/after-sales/records")
public class XingweiAfterSalesCaseController {
    private final XingweiAfterSalesCaseService cases;

    public XingweiAfterSalesCaseController(XingweiAfterSalesCaseService cases) {
        this.cases = cases;
    }

    @GetMapping
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public List<CaseRecord> records(@PathVariable String businessNo, Authentication authentication) {
        return cases.records(businessNo, authentication);
    }

    @GetMapping("/{recordId}/history")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public CaseHistory history(@PathVariable String businessNo, @PathVariable long recordId,
            Authentication authentication) {
        return cases.history(businessNo, recordId, authentication);
    }

    @PutMapping("/{recordId}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:manage')")
    public CaseRecord update(@PathVariable String businessNo, @PathVariable long recordId,
            @RequestBody UpdateRequest body, Authentication authentication, HttpServletRequest request) {
        if (body == null) throw new IllegalArgumentException("请求内容不能为空");
        return cases.update(businessNo, recordId, body.status(), body.handlingNote(), body.customerReply(),
                body.expectedVersion(), authentication, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        return Map.of("ok", false, "error", ex.getMessage() == null ? "请求参数不合法" : ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> forbidden(AccessDeniedException ex) {
        return Map.of("ok", false, "error", ex.getMessage() == null ? "超出当前数据范围" : ex.getMessage());
    }

    public record UpdateRequest(String status, String handlingNote, String customerReply,
            long expectedVersion) {}
}
