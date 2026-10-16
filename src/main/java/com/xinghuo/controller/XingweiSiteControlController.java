package com.xinghuo.controller;

import com.xinghuo.service.XingweiSiteControlService;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class XingweiSiteControlController {
    private final XingweiSiteControlService controls;
    public XingweiSiteControlController(XingweiSiteControlService controls){this.controls=controls;}
    @GetMapping("/admin/xingwei/site")
    @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication,'site:view','seo:view')")
    String site(){return "admin-xingwei-site";}
    @GetMapping("/admin/xingwei/frontend-controls")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'site:view')")
    String frontend(Model model,CsrfToken csrf,Authentication actor){
        model.addAttribute("csrf",csrf);model.addAttribute("username",actor.getName());model.addAttribute("xingweiMode",true);
        return "admin-frontend-controls";
    }
    @GetMapping("/api/admin/xingwei/site/access")
    @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication,'site:view','seo:view')")
    @ResponseBody
    Map<String,Object> access(Authentication actor){return controls.access(actor);}
    @GetMapping("/api/admin/xingwei/site/{area}")
    @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication,'site:view','seo:view')")
    @ResponseBody
    ResponseEntity<Object> read(@PathVariable String area,Authentication actor){var state=controls.read(area,actor);return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).eTag(state.revision()).body(state.values());}
    @PatchMapping({"/api/admin/xingwei/site/{area}","/api/admin/xingwei/site/{area}/{section}"})
    @PreAuthorize("@xingweiPermissionGuard.hasAny(authentication,'site:manage','seo:manage')")
    @ResponseBody
    ResponseEntity<Object> update(@PathVariable String area,@PathVariable(required=false) String section,
            @RequestBody Map<String,Object> body,@RequestHeader(value="If-Match",required=false) String revision,Authentication actor,HttpServletRequest request){
        var state=controls.update(area,section,body,revision,actor,request);return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).eTag(state.revision()).body(state.values());
    }
    @PostMapping("/api/admin/xingwei/site/images/{field}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'site:manage')")
    @ResponseBody
    Map<String,String> upload(@PathVariable String field,@RequestParam("file") MultipartFile file,Authentication actor,HttpServletRequest request){return controls.upload(field,file,actor,request);}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) @ResponseBody
    Map<String,Object> invalid(IllegalArgumentException e){return Map.of("error",e.getMessage());}
    @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN) @ResponseBody
    Map<String,Object> forbidden(AccessDeniedException e){return Map.of("error",e.getMessage());}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseStatus(HttpStatus.CONFLICT) @ResponseBody
    Map<String,Object> conflict(OptimisticLockingFailureException e){return Map.of("error",e.getMessage());}
}
