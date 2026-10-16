package com.xinghuo.controller;

import com.xinghuo.account.service.AccountException;
import com.xinghuo.service.XingweiAdminAccountService;
import com.xinghuo.service.XingweiAdminAccountService.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/xingwei/admin-accounts")
public class XingweiAdminAccountController {
    private final XingweiAdminAccountService accounts;
    public XingweiAdminAccountController(XingweiAdminAccountService accounts){this.accounts=accounts;}
    @ModelAttribute void privateResponse(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping public Object list(Authentication actor,@RequestParam(defaultValue="") String q){return accounts.snapshot(actor,q);}
    @PostMapping public Object create(@Valid @RequestBody CreateInput body,Authentication actor,HttpServletRequest request){accounts.create(body,actor,request);return Map.of("message","后台账号已创建，星维角色已分配");}
    @PatchMapping("/{id}") public Object update(@PathVariable long id,@Valid @RequestBody UpdateInput body,Authentication actor,HttpServletRequest request){accounts.update(id,body,actor,request);return Map.of("message","已保存，旧登录会话已撤销");}
    @PostMapping("/{id}/password") public Object password(@PathVariable long id,@Valid @RequestBody PasswordInput body,Authentication actor,HttpServletRequest request){accounts.password(id,body,actor,request);return Map.of("message","密码已重置，旧登录会话已撤销");}
    @ExceptionHandler(AccountException.class) ResponseEntity<?> accountError(AccountException error){return ResponseEntity.status(error.getStatus()).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(OptimisticLockingFailureException.class) ResponseEntity<?> conflict(OptimisticLockingFailureException error){return ResponseEntity.status(409).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> denied(AccessDeniedException error){return ResponseEntity.status(403).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(IllegalArgumentException error){return ResponseEntity.badRequest().body(Map.of("message",error.getMessage()));}
}
