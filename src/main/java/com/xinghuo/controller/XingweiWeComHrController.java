package com.xinghuo.controller;

import com.xinghuo.security.XingweiPermissionGuard;
import com.xinghuo.service.AuditService;
import com.xinghuo.wecom.XingweiWeComHrService;
import com.xinghuo.wecom.XingweiWeComHrAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** One outstanding preview per authenticated session; confirmation tokens are consumed once. */
@RestController
@RequestMapping("/api/admin/xingwei/wecom/hr")
public class XingweiWeComHrController {
    private static final String PENDING_KEY = XingweiWeComHrController.class.getName() + ".pending";
    private final XingweiWeComHrService hr;
    private final XingweiWeComHrAccessService localAccess;
    private final XingweiPermissionGuard permissions;
    private final AuditService audits;

    public XingweiWeComHrController(XingweiWeComHrService hr, XingweiWeComHrAccessService localAccess,
                                   XingweiPermissionGuard permissions, AuditService audits) {
        this.hr = hr;
        this.localAccess = localAccess;
        this.permissions = permissions;
        this.audits = audits;
    }

    @GetMapping("/capabilities")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'employee:view')")
    public Map<String, Object> capabilities(Authentication authentication) {
        return Map.of("enabled", hr.ready(), "canManage", canManage(authentication) && hr.ready());
    }

    @PostMapping("/preview")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'employee:manage')")
    public Map<String, Object> preview(@RequestBody XingweiWeComHrService.Command command,
                                       Authentication authentication, HttpServletRequest request) {
        requireManage(authentication);
        HttpSession session = request.getSession();
        synchronized (session) { session.removeAttribute(PENDING_KEY); }
        XingweiWeComHrService.Plan plan = hr.prepare(command);
        if (plan.action().equals("DISABLE_MEMBER")) localAccess.checkDisable(plan.targetId(), authentication.getName());
        String token = UUID.randomUUID().toString();
        synchronized (session) { session.setAttribute(PENDING_KEY, new Pending(token, authentication.getName(), plan)); }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("confirmationToken", token);
        result.put("name", plan.name());
        result.put("departmentName", plan.departmentName());
        result.put("positionName", plan.positionName());
        result.put("actionLabel", actionLabel(plan.action()));
        result.put("localAccessWillBeRevoked", plan.action().equals("DISABLE_MEMBER"));
        return result;
    }

    @PostMapping("/commit")
    @PreAuthorize("@xingweiPermissionGuard.has(authentication, 'employee:manage')")
    public Map<String, Object> commit(@RequestBody Confirmation body, Authentication authentication,
                                      HttpServletRequest request) {
        requireManage(authentication);
        if (!hr.ready()) throw new ResponseStatusException(HttpStatus.CONFLICT, "通讯录管理尚未启用");
        HttpSession session = request.getSession(false);
        Pending pending;
        if (session == null) throw stale();
        synchronized (session) {
            Object value = session.getAttribute(PENDING_KEY);
            if (!(value instanceof Pending stored) || body == null || !stored.token().equals(body.confirmationToken())
                    || !stored.operator().equals(authentication.getName()) || !Instant.now().isBefore(stored.plan().expiresAt())) throw stale();
            if (!stored.plan().name().equals(body.confirmName() == null ? "" : body.confirmName().trim()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入预览中的名称确认");
            pending = stored;
            session.removeAttribute(PENDING_KEY);
        }
        var plan = pending.plan();
        hr.validateCurrent(plan);
        // Audit intent before any external write, without names, contacts or payloads.
        if (plan.action().equals("DISABLE_MEMBER")) localAccess.beginDisable(plan.targetId(), authentication.getName(), request);
        else audits.log("XW_WECOM_HR_REQUEST", "WECOM_DIRECTORY", plan.targetId(), authentication.getName(),
                null, Map.of("title", actionLabel(plan.action()), "status", "待提交"), "核对后提交通讯录变更", request);
        try {
            hr.execute(plan);
        } catch (RuntimeException failure) {
            audits.log("XW_WECOM_HR_UNCONFIRMED", "WECOM_DIRECTORY", plan.targetId(), authentication.getName(),
                    null, Map.of("title", actionLabel(plan.action()), "status", "待核实"), "结果待核实，禁止自动重试", request);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, plan.action().equals("DISABLE_MEMBER")
                    ? "本地后台访问已停用，企业微信结果待核实；请刷新通讯录，勿重复提交"
                    : "操作结果待核实，请刷新通讯录或企业微信管理端核对，勿重复提交");
        }
        audits.log("XW_WECOM_HR_CONFIRMED", "WECOM_DIRECTORY", plan.targetId(), authentication.getName(),
                null, Map.of("title", actionLabel(plan.action()), "status", "已核对"), "通讯录回读核对完成", request);
        return Map.of("confirmed", true, "message", "通讯录变更已核对完成");
    }

    private boolean canManage(Authentication authentication) {
        return permissions.has(authentication, "employee:manage")
                && (permissions.resolve(authentication).allowAll() || "ALL".equals(permissions.resolve(authentication).dataScope()));
    }
    private void requireManage(Authentication authentication) {
        if (!canManage(authentication)) throw new AccessDeniedException("需要全企业员工管理权限");
    }
    private static String actionLabel(String action) {
        return switch (action) {
            case "CREATE_MEMBER" -> "新增员工";
            case "TRANSFER_MEMBER" -> "调整岗位";
            case "DISABLE_MEMBER" -> "停用员工";
            case "CREATE_DEPARTMENT" -> "新增部门";
            case "RENAME_DEPARTMENT" -> "部门改名";
            default -> "通讯录变更";
        };
    }
    private static ResponseStatusException stale() { return new ResponseStatusException(HttpStatus.CONFLICT, "变更预览已失效，请重新核对"); }
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, Object>> invalid(ResponseStatusException exception) {
        return org.springframework.http.ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("error", exception.getReason() == null ? "操作暂时无法完成，请刷新核对" : exception.getReason()));
    }
    private record Pending(String token, String operator, XingweiWeComHrService.Plan plan) {
        @Override public String toString() { return "Pending[private fields redacted]"; }
    }
    public record Confirmation(String confirmationToken, String confirmName) {}
}
