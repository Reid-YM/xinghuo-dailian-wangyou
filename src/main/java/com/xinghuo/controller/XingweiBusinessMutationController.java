package com.xinghuo.controller;

import com.xinghuo.dto.ApiDtos;
import com.xinghuo.service.XingweiBusinessMutationService;
import com.xinghuo.service.XingweiBusinessMutationService.QuoteChange;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/xingwei/business")
public class XingweiBusinessMutationController {
    private final XingweiBusinessMutationService business;
    public XingweiBusinessMutationController(XingweiBusinessMutationService business){this.business=business;}
    @PatchMapping("/quotes/{no}/status")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'quote:manage')")
    ApiDtos.QuoteAdminResponse quote(@PathVariable String no,@Valid @RequestBody QuoteChange body,Authentication actor,HttpServletRequest request){return business.updateQuote(no,body,actor,request);}
    @PostMapping("/quotes/{no}/convert")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'quote:manage') and @xingweiPermissionGuard.has(authentication,'order:manage')")
    ApiDtos.AdminOrderResponse convert(@PathVariable String no,@Valid @RequestBody QuoteChange body,Authentication actor,HttpServletRequest request){return business.convert(no,body,actor,request);}
    @PatchMapping("/orders/{no}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'order:manage')")
    ApiDtos.AdminOrderResponse order(@PathVariable String no,@Valid @RequestBody ApiDtos.UpdateOrderRequest body,Authentication actor,HttpServletRequest request){return business.updateOrder(no,body,actor,request);}
    @PostMapping("/orders")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'order:manage')")
    ApiDtos.AdminOrderResponse create(@Valid @RequestBody ApiDtos.CreateOrderRequest body,Authentication actor,HttpServletRequest request){return business.createOrder(body,actor,request);}
    @PostMapping("/orders/{no}/image")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'order:manage')")
    ApiDtos.AdminOrderResponse image(@PathVariable String no,@RequestParam long version,@RequestParam("file") MultipartFile file,Authentication actor,HttpServletRequest request){return business.attachImage(no,version,file,actor,request);}
    @GetMapping("/quotes/{no}/image")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'quote:view')")
    ResponseEntity<Resource> quoteImage(@PathVariable String no,Authentication actor){return business.quoteImage(no,actor);}
    @GetMapping("/orders/{no}/image")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication,'order:view')")
    ResponseEntity<Resource> orderImage(@PathVariable String no,Authentication actor){return business.orderImage(no,actor);}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String,Object> invalid(IllegalArgumentException e){return Map.of("error",e.getMessage());}
    @ExceptionHandler(AccessDeniedException.class) @ResponseStatus(HttpStatus.FORBIDDEN)
    Map<String,Object> forbidden(AccessDeniedException e){return Map.of("error",e.getMessage());}
    @ExceptionHandler(OptimisticLockingFailureException.class) @ResponseStatus(HttpStatus.CONFLICT)
    Map<String,Object> conflict(OptimisticLockingFailureException e){return Map.of("error",e.getMessage());}
}
