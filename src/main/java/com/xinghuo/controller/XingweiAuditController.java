package com.xinghuo.controller;

import com.xinghuo.service.XingweiAuditQueryService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@PreAuthorize("@xingweiPermissionGuard.has(authentication, 'audit:view')")
public class XingweiAuditController {
    private final XingweiAuditQueryService audits;
    public XingweiAuditController(XingweiAuditQueryService audits) {this.audits=audits;}
    @GetMapping("/admin/xingwei/audits")
    String page(){return "admin-xingwei-audits";}
    @GetMapping("/api/admin/xingwei/audits")
    @ResponseBody
    Map<String,Object> list(Authentication actor,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String action,
                            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(defaultValue="0") int page) {return audits.list(actor,q,action,from,to,page);}
    @GetMapping("/api/admin/xingwei/audits/{id}")
    @ResponseBody
    Map<String,Object> detail(@PathVariable long id,Authentication actor){return audits.detail(id,actor);}
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    Map<String,Object> invalid(IllegalArgumentException e){return Map.of("ok",false,"error",e.getMessage());}
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    Map<String,Object> forbidden(AccessDeniedException e){return Map.of("ok",false,"error",e.getMessage());}
}
