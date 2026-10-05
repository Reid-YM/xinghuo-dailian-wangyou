package com.xinghuo.account.web;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Comment;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.repo.AccountAuditRepository;
import com.xinghuo.account.model.Submission;
import com.xinghuo.account.service.CommentService;
import com.xinghuo.account.service.ContactNormalizer;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.PasswordPolicy;
import com.xinghuo.account.service.SubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户系统的后台管理接口。
 *
 * <p>挂在 {@code /api/admin/**} 下，因此由基线 SecurityConfig 的
 * {@code hasAnyRole("ADMIN","OPERATOR")} 保护，这里再加一层方法级注解做双保险。
 *
 * <p>注意：{@code /api/admin/**} <b>不在</b> SecurityConfig 的 CSRF 豁免名单里，
 * 前端调用这些写接口必须带 CSRF 头 —— 基线的 admin.js 已经在统一封装里处理。
 */
@RestController
@RequestMapping("/api/admin/account")
@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
public class AccountAdminApiController {

    private final CommentService comments;
    private final SubmissionService submissions;
    private final AccountRepository accounts;
    private final AccountAuditRepository audits;
    private static final SecureRandom PUBLIC_ID_RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public AccountAdminApiController(CommentService comments,
                                     SubmissionService submissions,
                                     AccountRepository accounts,
                                     AccountAuditRepository audits,
                                     PasswordEncoder passwordEncoder,
                                     JdbcTemplate jdbc) {
        this.comments = comments;
        this.submissions = submissions;
        this.accounts = accounts;
        this.audits = audits;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ 用户

    @GetMapping("/users")
    public List<UserAdminItem> users(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return accounts.findAll(PageRequest.of(safePage, safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(UserAdminItem::of)
                .toList();
    }

    @PostMapping("/users")
    public UserAdminItem createUser(@Valid @RequestBody UserCreateRequest body,
                                    Authentication auth) {
        ContactNormalizer.validateEmail(body.email());
        String emailNorm = ContactNormalizer.normalizeEmail(body.email());
        if (accounts.existsByEmailNorm(emailNorm)
                || accounts.existsByContactTypeAndContactNorm(Account.ContactType.EMAIL, emailNorm)) {
            throw AccountException.conflict("EMAIL_TAKEN", "该邮箱已存在");
        }
        PasswordPolicy.validate(body.password(), body.email());
        Account account = Account.builder()
                .contactType(Account.ContactType.EMAIL)
                .contactValue(body.email().trim())
                .contactNorm(emailNorm)
                .contactVerified(Boolean.TRUE.equals(body.emailVerified()))
                .publicUserId(generatePublicUserId())
                .email(body.email().trim())
                .emailNorm(emailNorm)
                .emailVerified(Boolean.TRUE.equals(body.emailVerified()))
                .passwordHash(passwordEncoder.encode(body.password()))
                .displayName(body.displayName().trim())
                .role(normalizeRole(body.role()))
                .levelCode("NEW")
                .status(Account.AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();
        account = accounts.save(account);
        return UserAdminItem.of(account);
    }

    @PostMapping("/users/{id}")
    public UserAdminItem updateUser(@PathVariable Long id,
                                    @Valid @RequestBody UserUpdateRequest body) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> AccountException.notFound("ACCOUNT_NOT_FOUND", "账号不存在"));
        if (body.displayName() != null && !body.displayName().isBlank()) {
            account.setDisplayName(body.displayName().trim());
        }
        if (body.role() != null && !body.role().isBlank()) {
            account.setRole(normalizeRole(body.role()));
        }
        if (body.status() != null && !body.status().isBlank()) {
            account.setStatus(parseAccountStatus(body.status()));
            if (account.getStatus() == Account.AccountStatus.DELETED && account.getDeletedAt() == null) {
                account.setDeletedAt(Instant.now());
            }
        }
        return UserAdminItem.of(accounts.save(account));
    }

    @PostMapping("/users/{id}/disable")
    public AccountDtos.OkResponse disableUser(@PathVariable Long id) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> AccountException.notFound("ACCOUNT_NOT_FOUND", "账号不存在"));
        account.setStatus(Account.AccountStatus.DISABLED);
        accounts.save(account);
        return AccountDtos.OkResponse.of("账号已禁用");
    }

    @DeleteMapping("/users/{id}")
    public AccountDtos.OkResponse deleteUser(@PathVariable Long id) {
        if (!accounts.existsById(id)) {
            throw AccountException.notFound("ACCOUNT_NOT_FOUND", "账号不存在");
        }
        jdbc.update("UPDATE quotes SET customer_account_id = NULL WHERE customer_account_id = ?", id);
        jdbc.update("UPDATE customer_orders SET customer_account_id = NULL WHERE customer_account_id = ?", id);
        accounts.deleteById(id);
        return AccountDtos.OkResponse.of("账号已彻底删除");
    }

    @PostMapping("/users/{id}/email-verified")
    public UserAdminItem setEmailVerified(@PathVariable Long id,
                                           @Valid @RequestBody EmailVerifiedRequest body) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> AccountException.notFound("ACCOUNT_NOT_FOUND", "账号不存在"));
        boolean verified = body.verified();
        account.setEmailVerified(verified);
        account.setContactVerified(verified);
        if (verified && account.getEmail() != null) {
            account.setContactType(Account.ContactType.EMAIL);
            account.setContactValue(account.getEmail());
            account.setContactNorm(account.getEmailNorm());
        }
        return UserAdminItem.of(accounts.save(account));
    }

    @PostMapping("/users/{id}/reset-password")
    public AccountDtos.OkResponse resetUserPassword(@PathVariable Long id,
                                                    @Valid @RequestBody PasswordResetRequest body) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> AccountException.notFound("ACCOUNT_NOT_FOUND", "账号不存在"));
        PasswordPolicy.validate(body.newPassword(), account.getEmail());
        account.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        accounts.save(account);
        return AccountDtos.OkResponse.of("密码已重置");
    }

    @GetMapping("/users/{id}/login-logs")
    public List<AuditItem> loginLogs(@PathVariable Long id,
                                     @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        return audits.findByUserIdOrderByCreatedAtDesc(id, PageRequest.of(0, safeSize)).stream()
                .filter(a -> a.getAction() != null && a.getAction().startsWith("LOGIN"))
                .map(AuditItem::of)
                .toList();
    }

    @GetMapping("/operation-logs")
    public List<AuditItem> operationLogs(@RequestParam(defaultValue = "100") int size) {
        int safeSize = Math.max(1, Math.min(size, 200));
        return audits.findAll(PageRequest.of(0, safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(AuditItem::of)
                .toList();
    }

    // ------------------------------------------------------------------ 评论

    @GetMapping("/comments")
    public List<AccountDtos.CommentItem> comments(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Comment.CommentStatus target = parseCommentStatus(status);
        return comments.listByStatus(target, page, size).stream()
                .map(c -> AccountDtos.CommentItem.of(c, null, null))
                .toList();
    }

    @PostMapping("/comments/{id}/review")
    public AccountDtos.OkResponse reviewComment(@PathVariable Long id,
                                                @Valid @RequestBody ReviewRequest body,
                                                Authentication auth) {
        comments.review(adminName(auth), id, body.approve(), body.note());
        return AccountDtos.OkResponse.of(body.approve() ? "评论已通过" : "评论已驳回");
    }

    @DeleteMapping("/comments/{id}")
    public AccountDtos.OkResponse deleteComment(@PathVariable Long id,
                                                @RequestParam(required = false) String note,
                                                Authentication auth) {
        comments.adminDelete(adminName(auth), id, note);
        return AccountDtos.OkResponse.of("评论已删除");
    }

    // ------------------------------------------------------------------ 投稿

    @GetMapping("/submissions")
    public List<AccountDtos.SubmissionItem> submissions(
            @RequestParam(defaultValue = "SUBMITTED") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return submissions.listByStatus(parseSubmissionStatus(status), page, size).stream()
                .map(AccountDtos.SubmissionItem::of)
                .toList();
    }

    @GetMapping("/submissions/{id}")
    public AccountDtos.SubmissionDetail submission(@PathVariable Long id) {
        return AccountDtos.SubmissionDetail.of(submissions.getAny(id));
    }

    @PostMapping("/submissions/{id}/review")
    public AccountDtos.SubmissionItem reviewSubmission(@PathVariable Long id,
                                                       @Valid @RequestBody SubmissionReviewRequest body,
                                                       Authentication auth) {
        Submission updated = submissions.review(adminName(auth), id,
                parseSubmissionStatus(body.status()), body.note());
        return AccountDtos.SubmissionItem.of(updated);
    }

    @PostMapping("/submissions/{id}/publish")
    public AccountDtos.SubmissionItem publishSubmission(@PathVariable Long id,
                                                        @Valid @RequestBody PublishRequest body,
                                                        Authentication auth) {
        return AccountDtos.SubmissionItem.of(
                submissions.markPublished(adminName(auth), id, body.slug()));
    }

    // ------------------------------------------------------------------ 工具

    /**
     * 管理员身份。后台登录用的是用户名（UserDetails.getUsername()），
     * 因此留痕存用户名而不是数字 id —— 后者需要反射去取 AdminUser.getId()，
     * 会把账号模块耦合到基线实体上。
     */
    private String adminName(Authentication auth) {
        return auth == null ? null : auth.getName();
    }

    private Comment.CommentStatus parseCommentStatus(String raw) {
        try {
            return Comment.CommentStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw com.xinghuo.account.service.AccountException
                    .badRequest("INVALID_STATUS", "不支持的评论状态");
        }
    }

    private Submission.SubmissionStatus parseSubmissionStatus(String raw) {
        try {
            return Submission.SubmissionStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw com.xinghuo.account.service.AccountException
                    .badRequest("INVALID_STATUS", "不支持的投稿状态");
        }
    }

    private String normalizeRole(String raw) {
        String role = raw == null || raw.isBlank() ? "BUYER" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (role) {
            case "BUYER", "SELLER", "BOOSTER", "ADMIN", "OPERATOR" -> role;
            default -> throw AccountException.badRequest("INVALID_ROLE", "不支持的角色");
        };
    }

    private String generatePublicUserId() {
        for (int i = 0; i < 8; i++) {
            String candidate = "XH" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT)
                    + Integer.toString(PUBLIC_ID_RANDOM.nextInt(36 * 36 * 36), 36).toUpperCase(Locale.ROOT);
            if (!accounts.existsByPublicUserId(candidate)) {
                return candidate;
            }
        }
        throw AccountException.serverError("USER_ID_GENERATE_FAILED", "用户 ID 生成失败，请稍后重试");
    }

    private Account.AccountStatus parseAccountStatus(String raw) {
        try {
            return Account.AccountStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw AccountException.badRequest("INVALID_STATUS", "不支持的账号状态");
        }
    }

    // ------------------------------------------------------------------ 请求体

    public record ReviewRequest(boolean approve, @Size(max = 300) String note) {
    }

    public record SubmissionReviewRequest(@NotBlank String status,
                                          @Size(max = 500) String note) {
    }

    public record PublishRequest(@NotBlank @Size(max = 140) String slug) {
    }

    public record UserAdminItem(Long id, String publicUserId, String email, boolean emailVerified, String displayName,
                                String role, String status, Instant createdAt, Instant updatedAt,
                                Instant lastLoginAt) {
        static UserAdminItem of(Account a) {
            return new UserAdminItem(a.getId(), a.getPublicUserId(), a.getEmail(), a.isEmailVerified(),
                    a.getDisplayName(), a.getRole(), a.getStatus().name(),
                    a.getCreatedAt(), a.getUpdatedAt(), a.getLastLoginAt());
        }
    }

    public record AuditItem(Long id, Long userId, String action, String detail,
                            String ip, String userAgent, Instant createdAt) {
        static AuditItem of(com.xinghuo.account.model.AccountAudit a) {
            return new AuditItem(a.getId(), a.getUserId(), a.getAction(), a.getDetail(),
                    a.getIp(), a.getUserAgent(), a.getCreatedAt());
        }
    }

    public record UserCreateRequest(@NotBlank @jakarta.validation.constraints.Email @Size(max = 160) String email,
                                    @NotBlank @Size(max = 60) String displayName,
                                    @NotBlank @Size(max = 72) String password,
                                    @Size(max = 24) String role,
                                    Boolean emailVerified) {
    }

    public record UserUpdateRequest(@Size(max = 60) String displayName,
                                    @Size(max = 24) String role,
                                    @Size(max = 16) String status) {
    }

    public record PasswordResetRequest(@NotBlank @Size(max = 72) String newPassword) {
    }

    public record EmailVerifiedRequest(boolean verified) {
    }

}
