package com.xinghuo.controller;

import com.xinghuo.service.XingweiAfterSalesAttentionQueueService;
import com.xinghuo.service.XingweiAfterSalesAttentionQueueService.AttentionQueueSnapshot;
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

/** Read-only filtered queue for the V4J attention aging buckets. */
@RestController
@RequestMapping("/api/admin/xingwei/business/after-sales/cases")
public class XingweiAfterSalesAttentionQueueController {
    private final XingweiAfterSalesAttentionQueueService queue;

    public XingweiAfterSalesAttentionQueueController(XingweiAfterSalesAttentionQueueService queue) {
        this.queue = queue;
    }

    @GetMapping("/attention-queue")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public AttentionQueueSnapshot queue(@RequestParam String age,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String followup,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "NEWEST_FIRST") String sort,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication) {
        return queue.queue(age, status, category, followup, q, sort, page, authentication);
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
