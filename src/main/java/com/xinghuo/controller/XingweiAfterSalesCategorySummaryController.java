package com.xinghuo.controller;

import com.xinghuo.service.XingweiAfterSalesCategorySummaryService;
import com.xinghuo.service.XingweiAfterSalesCategorySummaryService.CategorySummary;
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

/** Read-only issue-type distribution for the scoped after-sales queue. */
@RestController
@RequestMapping("/api/admin/xingwei/business/after-sales/cases")
public class XingweiAfterSalesCategorySummaryController {
    private final XingweiAfterSalesCategorySummaryService categories;

    public XingweiAfterSalesCategorySummaryController(XingweiAfterSalesCategorySummaryService categories) {
        this.categories = categories;
    }

    @GetMapping("/category-summary")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public CategorySummary summary(Authentication authentication) {
        return categories.summary(authentication);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> forbidden(AccessDeniedException ex) {
        return Map.of("ok", false, "error", ex.getMessage() == null ? "超出当前数据范围" : ex.getMessage());
    }
}
