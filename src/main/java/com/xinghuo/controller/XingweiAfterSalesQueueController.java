package com.xinghuo.controller;

import com.xinghuo.service.XingweiAfterSalesCaseService;
import com.xinghuo.service.XingweiAfterSalesCaseService.QueueSnapshot;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Scoped queue of individual V70 after-sales submissions and V4A handling states. */
@RestController
@RequestMapping("/api/admin/xingwei/business/after-sales/cases")
public class XingweiAfterSalesQueueController {
    private final XingweiAfterSalesCaseService cases;

    public XingweiAfterSalesQueueController(XingweiAfterSalesCaseService cases) {
        this.cases = cases;
    }

    @GetMapping
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public QueueSnapshot queue(@RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String followup,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication) {
        return cases.queue(status, category, followup, q, page, authentication);
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
}
