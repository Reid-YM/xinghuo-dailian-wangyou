package com.xinghuo.controller;

import com.xinghuo.exception.NotFoundException;
import com.xinghuo.service.XingweiPaymentWorkspaceService;
import com.xinghuo.service.XingweiPaymentWorkspaceService.PrepareInput;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class XingweiPaymentWorkspaceController {
    private final XingweiPaymentWorkspaceService payments;
    public XingweiPaymentWorkspaceController(XingweiPaymentWorkspaceService payments){this.payments=payments;}
    @ModelAttribute void privateResponse(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping("/admin/xingwei/payments") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'payment:view')")
    String page(Model model,CsrfToken csrf){model.addAttribute("csrf",csrf);return "admin-xingwei-payments";}
    @GetMapping("/api/admin/xingwei/payments") @ResponseBody Object list(Authentication actor,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page){return payments.snapshot(actor,q,page);}
    @GetMapping("/api/admin/xingwei/payments/orders/{no}") @ResponseBody Object order(@PathVariable String no,Authentication actor){
        try {
            return payments.order(no,actor);
        } catch (NotFoundException notFound) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", "ORDER_NOT_FOUND",
                    "message", "未找到履约订单。请核对订单号或业务编号；仅提交询价尚未接受正式报价时不会产生订单"));
        }
    }
    @PostMapping("/api/admin/xingwei/payments/orders/{no}") @ResponseBody Object prepare(@PathVariable String no,@Valid @RequestBody PrepareInput body,@RequestHeader(value="If-Match",required=false) String expected,Authentication actor,HttpServletRequest request){return payments.prepare(no,body,expected,actor,request);}
    @PostMapping("/api/admin/xingwei/payments/{no}/close") @ResponseBody Object close(@PathVariable String no,@RequestHeader(value="If-Match",required=false) String expected,Authentication actor,HttpServletRequest request){return payments.close(no,expected,actor,request);}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseBody ResponseEntity<?> conflict(OptimisticLockingFailureException error){return ResponseEntity.status(409).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(AccessDeniedException.class) @ResponseBody ResponseEntity<?> denied(AccessDeniedException error){return ResponseEntity.status(403).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseBody ResponseEntity<?> bad(IllegalArgumentException error){return ResponseEntity.badRequest().body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) @ResponseBody ResponseEntity<?> state(IllegalStateException error){return ResponseEntity.status(409).body(Map.of("message",error.getMessage()));}
}
