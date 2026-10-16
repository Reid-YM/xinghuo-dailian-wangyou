package com.xinghuo.controller;

import com.xinghuo.service.XingweiChannelOperationsService;
import com.xinghuo.service.XingweiChannelOperationsService.ChannelState;
import com.xinghuo.service.XingweiChannelOperationsService.StateInput;
import com.xinghuo.service.XingweiChannelOperationsService.Task;
import com.xinghuo.service.XingweiChannelOperationsService.TaskInput;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class XingweiChannelOperationsController {
    private final XingweiChannelOperationsService channels;
    public XingweiChannelOperationsController(XingweiChannelOperationsService channels) { this.channels = channels; }

    @GetMapping("/admin/xingwei/channels")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'channel:view')")
    String page() { return "admin-xingwei-channels"; }

    @GetMapping("/api/admin/xingwei/channels")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'channel:view')")
    @ResponseBody
    Map<String, Object> snapshot(Authentication actor, @RequestParam(defaultValue="") String channel,
                                 @RequestParam(defaultValue="") String status, @RequestParam(defaultValue="") String q,
                                 @RequestParam(defaultValue="0") int page) {
        return channels.snapshot(actor, channel, status, q, page);
    }

    @PostMapping("/api/admin/xingwei/channels/tasks")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'channel:manage')")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    Task create(Authentication actor, @RequestBody TaskInput input, HttpServletRequest request) {
        return channels.create(actor, input, request);
    }

    @PutMapping("/api/admin/xingwei/channels/tasks/{id}")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'channel:manage')")
    @ResponseBody
    Task update(@PathVariable String id, Authentication actor, @RequestBody TaskInput input, HttpServletRequest request) {
        return channels.update(id, actor, input, request);
    }

    @PutMapping("/api/admin/xingwei/channels/{channel}/state")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'channel:manage')")
    @ResponseBody
    ChannelState updateState(@PathVariable String channel, Authentication actor, @RequestBody StateInput input,
                             HttpServletRequest request) {
        return channels.updateChannel(channel, actor, input, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    Map<String, Object> badRequest(IllegalArgumentException ex) { return Map.of("ok", false, "error", ex.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    Map<String, Object> forbidden(AccessDeniedException ex) { return Map.of("ok", false, "error", ex.getMessage()); }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    Map<String, Object> conflict(OptimisticLockingFailureException ex) { return Map.of("ok", false, "error", ex.getMessage()); }
}
