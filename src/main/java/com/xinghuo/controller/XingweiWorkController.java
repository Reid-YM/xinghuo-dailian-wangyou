package com.xinghuo.controller;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.XingweiWorkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 星维 V2F：派单领取与员工“我的工作”受控接口。 */
@RestController
@RequestMapping("/api/admin/xingwei/work")
public class XingweiWorkController {
    private final XingweiWorkService work;

    public XingweiWorkController(XingweiWorkService work) {
        this.work = work;
    }

    @GetMapping
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:view')")
    public Map<String, Object> snapshot(Authentication authentication) {
        return work.snapshot(authentication);
    }

    @PutMapping("/orders/{businessNo}/claim")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:assign')")
    public Map<String, Object> claim(
            @PathVariable String businessNo,
            Authentication authentication,
            HttpServletRequest request) {
        return work.claim(businessNo, authentication, request);
    }

    @PatchMapping("/orders/{businessNo}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'order:manage')")
    public ApiDtos.AdminOrderResponse updateOrder(
            @PathVariable String businessNo,
            @Valid @RequestBody ApiDtos.UpdateOrderRequest body,
            Authentication authentication,
            HttpServletRequest request) {
        return work.updateOrder(businessNo, body, authentication, request);
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
}
