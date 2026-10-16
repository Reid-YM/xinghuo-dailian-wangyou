package com.xinghuo.controller;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.XingweiCatalogDetailsService;
import com.xinghuo.service.XingweiCatalogDetailsService.*;
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
import org.springframework.web.multipart.MultipartFile;

@Controller
public class XingweiCatalogDetailsController {
    private final XingweiCatalogDetailsService details;
    public XingweiCatalogDetailsController(XingweiCatalogDetailsService details){this.details=details;}
    @ModelAttribute void privateResponse(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping("/admin/xingwei/catalog-details") @PreAuthorize("@xingweiPermissionGuard.has(authentication,'service:view')") String page(Model model,CsrfToken csrf){model.addAttribute("csrf",csrf);return "admin-xingwei-catalog-details";}
    @GetMapping("/api/admin/xingwei/catalog-details") @ResponseBody Object snapshot(Authentication actor){return details.snapshot(actor);}
    @PutMapping("/api/admin/xingwei/catalog-details/services/{id}") @ResponseBody Object service(@PathVariable long id,@Valid @RequestBody ApiDtos.ServiceUpsert body,@RequestHeader(value="If-Match",required=false) String expected,Authentication actor,HttpServletRequest request){return details.updateService(id,body,expected,actor,request);}
    @PutMapping("/api/admin/xingwei/catalog-details/games/{code}") @ResponseBody Object game(@PathVariable String code,@Valid @RequestBody ApiDtos.GameProfileUpdateRequest body,@RequestHeader(value="If-Match",required=false) String expected,Authentication actor,HttpServletRequest request){return details.updateGame(code,body,expected,actor,request);}
    @PostMapping("/api/admin/xingwei/catalog-details/games/bulk") @ResponseBody Object bulk(@Valid @RequestBody BulkInput body,Authentication actor,HttpServletRequest request){details.bulk(body,actor,request);return Map.of("message","批量处理已完成");}
    @DeleteMapping("/api/admin/xingwei/catalog-details/{kind}/{key}") @ResponseBody ResponseEntity<?> delete(@PathVariable String kind,@PathVariable String key,@Valid @RequestBody Confirmation body,@RequestHeader(value="If-Match",required=false) String expected,Authentication actor,HttpServletRequest request){details.delete(kind,key,body,expected,actor,request);return ResponseEntity.noContent().build();}
    @PostMapping("/api/admin/xingwei/catalog-details/images/{kind}") @ResponseBody Object image(@PathVariable String kind,@RequestParam MultipartFile file,Authentication actor,HttpServletRequest request){return details.image(kind,file,actor,request);}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseBody ResponseEntity<?> conflict(OptimisticLockingFailureException error){return ResponseEntity.status(409).body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(AccessDeniedException.class) @ResponseBody ResponseEntity<?> denied(AccessDeniedException error){return ResponseEntity.status(403).body(Map.of("message",error.getMessage()));}
}
