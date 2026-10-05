package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountSession;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.AccountAuditService;
import com.xinghuo.account.service.Masking;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.account.service.AccountService;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号 API。
 *
 * <p>取代基线 jar 中被删除的 {@code CustomerAccountController}，路径保持
 * {@code /api/account/**} 不变，因此前端与既有链接无需改动。
 */
@RestController
@RequestMapping("/api/account")
public class AccountApiController {

    private final AccountService accountService;
    private final AccountSessionService sessionService;
    private final AccountAuditService audit;
    private final AccountRepository accounts;
    private final UploadService uploads;

    public AccountApiController(AccountService accountService,
                                AccountSessionService sessionService,
                                AccountAuditService audit,
                                AccountRepository accounts,
                                UploadService uploads) {
        this.accountService = accountService;
        this.sessionService = sessionService;
        this.audit = audit;
        this.accounts = accounts;
        this.uploads = uploads;
    }

    // ------------------------------------------------------------- 公开端点

    @PostMapping("/register")
    public AccountDtos.ProfileResponse register(@Valid @RequestBody AccountDtos.RegisterRequest body,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        Account account = accountService.register(body, request, response);
        return profile(account);
    }

    @PostMapping("/login")
    public AccountDtos.ProfileResponse login(@Valid @RequestBody AccountDtos.LoginRequest body,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        Account account = accountService.login(body, request, response);
        return profile(account);
    }

    @PostMapping("/logout")
    public AccountDtos.OkResponse logout(HttpServletRequest request, HttpServletResponse response) {
        accountService.logout(request, response);
        return AccountDtos.OkResponse.of("已退出登录");
    }

    // ------------------------------------------------------------- 登录端点

    @GetMapping("/me")
    @RequireAccount
    public AccountDtos.ProfileResponse me(HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return profile(account);
    }

    /**
     * 统一的档案响应。
     *
     * <p>注册、登录、/me 三处必须走同一个方法：曾经注册/登录直接传了空权益列表，
     * 导致同一个账号在不同接口下拿到的权益不一致，前端会先看到空权益再看到真实权益。
     */
    private AccountDtos.ProfileResponse profile(Account account) {
        return AccountDtos.ProfileResponse.of(account, List.of());
    }

    @PostMapping("/profile")
    @RequireAccount
    @Transactional
    public AccountDtos.ProfileResponse updateProfile(@Valid @RequestBody AccountDtos.UpdateProfileRequest body,
                                                     HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        String displayName = body.displayName() == null ? "" : body.displayName().trim();
        if (displayName.isEmpty()) {
            throw AccountException.badRequest("INVALID_DISPLAY_NAME", "请填写昵称");
        }
        account.setDisplayName(displayName);
        account.setAvatarUrl(normalizeAvatarUrl(body.avatarUrl()));
        Account saved = accounts.save(account);
        audit.record(saved.getId(), "PROFILE_UPDATED", "profile", request);
        return profile(saved);
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequireAccount
    @Transactional
    public AccountDtos.ProfileResponse uploadAvatar(@RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        UploadAsset asset = uploads.storeImage(file, UploadAsset.Kind.USER_AVATAR);
        uploads.bind(asset.getId(), "ACCOUNT_AVATAR", String.valueOf(account.getId()));
        account.setAvatarUrl("/api/site-assets/" + asset.getId());
        Account saved = accounts.save(account);
        audit.record(saved.getId(), "PROFILE_AVATAR_UPDATED", "profile", request);
        return profile(saved);
    }

    private static String normalizeAvatarUrl(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.startsWith("/") || value.startsWith("https://")) return value;
        throw AccountException.badRequest("INVALID_AVATAR_URL", "头像地址需为站内路径或 https 链接");
    }

    @PostMapping("/password/change")
    @RequireAccount
    public AccountDtos.OkResponse changePassword(@Valid @RequestBody AccountDtos.ChangePasswordRequest body,
                                                 HttpServletRequest request,
                                                 HttpServletResponse response) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        accountService.changePassword(account, body, request, response);
        return AccountDtos.OkResponse.of("密码已修改，请重新登录");
    }

    // ------------------------------------------------------------- 设备管理

    @GetMapping("/sessions")
    @RequireAccount
    public List<AccountDtos.SessionResponse> sessions(HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        String currentId = sessionService.currentSessionId(request).orElse(null);
        return sessionService.listActive(account.getId()).stream()
                .map(s -> new AccountDtos.SessionResponse(
                        s.getId(),
                        Masking.ip(s.getIp()),
                        s.getDeviceLabel(),
                        s.getCreatedAt(),
                        s.getLastSeenAt(),
                        s.getId().equals(currentId)))
                .toList();
    }

    @DeleteMapping("/sessions/{id}")
    @RequireAccount
    public AccountDtos.OkResponse revokeSession(@PathVariable String id,
                                                HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        AccountSession target = sessionService.listActive(account.getId()).stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                // 只能下线自己的会话；别人的 id 一律当作不存在，避免越权枚举。
                .orElseThrow(() -> AccountException.notFound("SESSION_NOT_FOUND", "会话不存在或已下线"));
        sessionService.revoke(target.getId());
        audit.record(account.getId(), "SESSION_REVOKED", "target=current:" + target.getId().equals(
                sessionService.currentSessionId(request).orElse(null)), request);
        return AccountDtos.OkResponse.of("该设备已下线");
    }

    @PostMapping("/sessions/revoke-all")
    @RequireAccount
    public AccountDtos.OkResponse revokeAll(HttpServletRequest request, HttpServletResponse response) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        String currentId = sessionService.currentSessionId(request)
                .orElseThrow(AccountException::unauthenticated);
        int count = sessionService.revokeOthers(account.getId(), currentId);
        audit.record(account.getId(), "SESSIONS_REVOKED_OTHERS", "count=" + count, request);
        return AccountDtos.OkResponse.of("已下线其他 " + count + " 个设备");
    }

    @GetMapping("/security/events")
    @RequireAccount
    public List<AccountDtos.SecurityEventResponse> securityEvents(HttpServletRequest request) {
        Account account = AccountAuthInterceptor.requireAccount(request);
        return audit.recentForUser(account.getId(), 10).stream()
                .map(row -> new AccountDtos.SecurityEventResponse(
                        row.getAction(), eventTitle(row.getAction()), eventDescription(row.getAction()),
                        Masking.ip(row.getIp()), RequestUtils.deviceLabel(row.getUserAgent()), row.getCreatedAt()))
                .toList();
    }

    private static String eventTitle(String action) {
        return switch (action == null ? "" : action) {
            case "REGISTER" -> "账号已创建";
            case "LOGIN_OK" -> "登录成功";
            case "LOGOUT" -> "退出登录";
            case "PWD_CHANGE" -> "登录密码已修改";
            case "PWD_RESET" -> "登录密码已重置";
            case "EMAIL_VERIFY_SENT" -> "已发送邮箱验证";
            case "EMAIL_VERIFIED" -> "邮箱验证成功";
            case "PROFILE_UPDATED" -> "账号资料已更新";
            case "PROFILE_AVATAR_UPDATED" -> "头像已更新";
            case "SESSION_REVOKED" -> "设备已下线";
            case "SESSIONS_REVOKED_OTHERS" -> "其他设备已全部下线";
            default -> "账号安全活动";
        };
    }

    private static String eventDescription(String action) {
        return switch (action == null ? "" : action) {
            case "PWD_CHANGE", "PWD_RESET" -> "所有旧会话已失效，需要重新登录。";
            case "EMAIL_VERIFIED" -> "安全邮箱现可用于找回密码。";
            case "SESSION_REVOKED", "SESSIONS_REVOKED_OTHERS" -> "指定登录会话已失效。";
            case "LOGIN_OK" -> "账号凭据验证通过并建立登录会话。";
            default -> "该操作已写入账号安全记录。";
        };
    }
}
