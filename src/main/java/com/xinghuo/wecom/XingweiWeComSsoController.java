package com.xinghuo.wecom;

import com.xinghuo.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/** 企业微信内静默授权入口；只为具备星维角色的活跃员工建立后台 Session。 */
@RestController
@RequestMapping("/admin/xingwei/wecom")
public class XingweiWeComSsoController {
    private static final String STATE_KEY = XingweiWeComSsoController.class.getName() + ".state";
    public static final String SSO_SESSION_KEY = XingweiWeComSsoController.class.getName() + ".user";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HttpSessionSecurityContextRepository CONTEXT_REPOSITORY =
            new HttpSessionSecurityContextRepository();

    private final XingweiWeComProperties properties;
    private final XingweiWeComDirectoryClient client;
    private final XingweiWeComBindingService bindings;
    private final XingweiWeComPositionRoleService positionRoles;
    private final AuditService audits;
    private final String baseUrl;

    public XingweiWeComSsoController(
            XingweiWeComProperties properties,
            XingweiWeComDirectoryClient client,
            XingweiWeComBindingService bindings,
            XingweiWeComPositionRoleService positionRoles,
            AuditService audits,
            @Value("${app.base-url}") String baseUrl) {
        this.properties = properties;
        this.client = client;
        this.bindings = bindings;
        this.positionRoles = positionRoles;
        this.audits = audits;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        requireReady();
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        request.getSession(true).setAttribute(STATE_KEY, new PendingState(state, Instant.now()));
        response.setHeader("Cache-Control", "no-store");
        String callback = callbackUrl();
        String authorize = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
                + encode(properties.corpId()) + "&redirect_uri=" + encode(callback)
                + "&response_type=code&scope=snsapi_base&state=" + encode(state)
                + "&agentid=" + encode(properties.agentId()) + "#wechat_redirect";
        response.sendRedirect(authorize);
    }

    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request, HttpServletResponse response)
            throws java.io.IOException {
        requireReady();
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        HttpSession session = request.getSession(false);
        PendingState pending = session == null ? null : (PendingState) session.getAttribute(STATE_KEY);
        if (session != null) {
            session.removeAttribute(STATE_KEY);
        }
        if (pending == null || state == null || !MessageDigest.isEqual(
                pending.value().getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8))
                || pending.createdAt().plusSeconds(300).isBefore(Instant.now())
                || code == null || code.isBlank() || code.length() > 512) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        UserDetails user;
        String wecomUserId;
        try {
            wecomUserId = client.userIdForCode(code);
            Long adminUserId = bindings.boundAdminUserId(wecomUserId);
            XingweiWeComDirectoryClient.Member member = null;
            if (adminUserId == null || (properties.autoRoleEnabled() && positionRoles.needsRefresh(adminUserId))) {
                try {
                    member = client.member(wecomUserId);
                } catch (RuntimeException unavailable) {
                    if (adminUserId != null && properties.autoRoleEnabled() && positionRoles.autoManaged(adminUserId)) {
                        throw unavailable;
                    }
                    // OAuth still proves the UserID. Without verified title, register only as pending.
                    member = new XingweiWeComDirectoryClient.Member(wecomUserId, wecomUserId, "");
                }
            }
            if (adminUserId == null) {
                adminUserId = bindings.registerPending(wecomUserId, member.name());
                audits.log("XW_WECOM_ENROLL_PENDING", "ADMIN_USER", String.valueOf(adminUserId),
                        "wecom", null, null, "企业微信成员已登记，等待星维角色分配", request);
            }
            if (properties.autoRoleEnabled() && member != null && positionRoles.sync(adminUserId, member.position())) {
                audits.log("XW_WECOM_POSITION_ROLE_SYNC", "ADMIN_USER", String.valueOf(adminUserId),
                        "wecom", null, null, "星维岗位角色已根据企业微信职务同步", request);
            }
            try {
                user = bindings.loginUser(wecomUserId);
            } catch (IllegalArgumentException notApproved) {
                session.invalidate();
                audits.log("XW_WECOM_SSO_PENDING", "ADMIN_USER", String.valueOf(adminUserId),
                        "wecom", null, null, "星维角色或账号状态尚未满足登录条件", request);
                response.sendRedirect("/admin/xingwei/wecom/pending");
                return;
            }
        } catch (RuntimeException ex) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }
        // Rotate the anonymous session and CSRF token before saving Spring Security's context.
        session.invalidate();
        String role = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> "ROLE_ADMIN".equals(authority) || "ROLE_OPERATOR".equals(authority))
                .findFirst().orElseThrow();
        request.getSession(true).setAttribute(SSO_SESSION_KEY,
                new SsoIdentity(user.getUsername(), wecomUserId, role.substring(5)));
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        CONTEXT_REPOSITORY.saveContext(context, request, response);
        audits.log("XW_WECOM_SSO_LOGIN", "ADMIN_USER", user.getUsername(), user.getUsername(),
                null, null, "企业微信自建应用免登录", request);
        response.sendRedirect("/admin/xingwei/work");
    }

    @GetMapping("/pending")
    public ModelAndView pending() {
        requireReady();
        return new ModelAndView("admin-xingwei-wecom-pending");
    }

    @PutMapping("/bindings/{adminUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bind(@PathVariable long adminUserId, @RequestBody BindingRequest body,
            Authentication authentication, HttpServletRequest request) {
        try {
            bindings.bind(adminUserId, body.wecomUserId());
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        audits.log("XW_WECOM_BINDING_UPDATE", "ADMIN_USER", String.valueOf(adminUserId),
                authentication.getName(), null, null, "企业微信账号绑定已更新", request);
    }

    @GetMapping("/bindings/{adminUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public BindingRequest binding(@PathVariable long adminUserId) {
        try {
            return new BindingRequest(bindings.bindingFor(adminUserId));
        } catch (IllegalArgumentException ex) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void requireReady() {
        if (!properties.ssoReady()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private String callbackUrl() {
        URI base = URI.create(baseUrl);
        if (!"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null
                || base.getUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null) {
            throw new IllegalStateException("企业微信免登录需要有效的 HTTPS APP_BASE_URL");
        }
        return base.resolve("/admin/xingwei/wecom/callback").toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record BindingRequest(String wecomUserId) {}
    public record SsoIdentity(String username, String wecomUserId, String role) implements java.io.Serializable {}
    private record PendingState(String value, Instant createdAt) implements java.io.Serializable {}
}
