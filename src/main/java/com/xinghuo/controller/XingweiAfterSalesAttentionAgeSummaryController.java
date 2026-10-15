package com.xinghuo.controller;

import com.xinghuo.service.XingweiAfterSalesAttentionAgeSummaryService;
import com.xinghuo.service.XingweiAfterSalesAttentionAgeSummaryService.AttentionAgeSummary;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Read-only attention aging distribution for the scoped after-sales queue. */
@RestController
@RequestMapping("/api/admin/xingwei/business/after-sales/cases")
public class XingweiAfterSalesAttentionAgeSummaryController {
    private final XingweiAfterSalesAttentionAgeSummaryService aging;

    public XingweiAfterSalesAttentionAgeSummaryController(XingweiAfterSalesAttentionAgeSummaryService aging) {
        this.aging = aging;
    }

    @GetMapping("/attention-age-summary")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public AttentionAgeSummary summary(Authentication authentication) {
        return aging.summary(authentication);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> forbidden(AccessDeniedException ex) {
        return Map.of("ok", false, "error", ex.getMessage() == null ? "超出当前数据范围" : ex.getMessage());
    }
}
