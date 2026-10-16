package com.xinghuo.controller;

import com.xinghuo.service.XingweiBusinessMaintenanceService;
import com.xinghuo.service.XingweiBusinessMaintenanceService.*;
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
public class XingweiBusinessMaintenanceController {
    private final XingweiBusinessMaintenanceService work;
    public XingweiBusinessMaintenanceController(XingweiBusinessMaintenanceService work){this.work=work;}
    @ModelAttribute void privateResponse(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping("/admin/xingwei/maintenance") @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication,'order:view','quote:view')") String page(Model model,CsrfToken csrf){model.addAttribute("csrf",csrf);return "admin-xingwei-maintenance";}
    @GetMapping("/api/admin/xingwei/maintenance/access") @ResponseBody Object access(Authentication actor){return work.access(actor);}
    @GetMapping("/api/admin/xingwei/maintenance") @ResponseBody Object rows(@RequestParam(defaultValue="orders") String type,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Authentication actor){return work.snapshot(type,q,page,actor);}
    @PatchMapping("/api/admin/xingwei/maintenance/orders/{key}/case") @ResponseBody Object publicCase(@PathVariable String key,@Valid @RequestBody PublicCaseInput body,@RequestHeader(value="If-Match",required=false) String expected,Authentication actor,HttpServletRequest request){return work.publicCase(key,body,expected,actor,request);}
    @DeleteMapping("/api/admin/xingwei/maintenance/{type}/{key}") @ResponseBody ResponseEntity<?> delete(@PathVariable String type,@PathVariable String key,@Valid @RequestBody DeleteInput body,@RequestHeader(value="If-Match",required=false) String expected,Authentication actor,HttpServletRequest request){work.delete(type,key,body,expected,actor,request);return ResponseEntity.noContent().build();}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseBody ResponseEntity<?> conflict(OptimisticLockingFailureException error){return ResponseEntity.status(409).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(AccessDeniedException.class) @ResponseBody ResponseEntity<?> denied(AccessDeniedException error){return ResponseEntity.status(403).body(Map.of("message",error.getMessage()));}
}
