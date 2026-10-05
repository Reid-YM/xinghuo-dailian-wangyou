package com.xinghuo.account.service;

import com.xinghuo.account.dto.AccountDtos;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.repo.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号核心业务：注册、登录、登出、改密。
 *
 * <p>限流采用「IP + 账号」双维度：只按 IP 限，攻击者换代理即可绕过；
 * 只按账号限，会被人拿来故意锁死他人账号。两个维度都要有，且阈值不同。
 *
 * <p>失败计数存在数据库（customer_accounts.failed_attempts），不是 HttpSession ——
 * 基线实现把计数放在会话里，用户清掉 Cookie 就重置了，等于没有防爆破。
 */
@Service
public class AccountService {

    private static final int LOGIN_LIMIT_PER_IP = 20;
    private static final Duration LOGIN_IP_WINDOW = Duration.ofMinutes(15);
    private static final int REGISTER_LIMIT_PER_IP = 5;
    private static final Duration REGISTER_IP_WINDOW = Duration.ofHours(1);
    private static final SecureRandom PUBLIC_ID_RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final AccountSessionService sessionService;
    private final AccountAuditService audit;
    private final RateLimiter rateLimiter;
    private final AccountLockService lockService;
    private final AccountEmailService emailService;

    /**
     * 账号不存在时用来「陪跑」一次 BCrypt 校验的假哈希。
     *
     * <p>不这样做的话，「账号不存在」会直接返回，而「账号存在」要跑一次
     * cost=12 的 BCrypt（约 200–400 ms），两者差两个数量级，公网可稳定测量出
     * 某个联系方式是否注册过。启动时生成一次，避免每次请求都算。
     */
    private final String timingDecoyHash;

    public AccountService(AccountRepository accounts,
                          PasswordEncoder encoder,
                          AccountSessionService sessionService,
                          AccountAuditService audit,
                          RateLimiter rateLimiter,
                          AccountLockService lockService,
                          AccountEmailService emailService) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.sessionService = sessionService;
        this.audit = audit;
        this.rateLimiter = rateLimiter;
        this.lockService = lockService;
        this.emailService = emailService;
        this.timingDecoyHash = encoder.encode("timing-equalization-decoy");
    }

    // ------------------------------------------------------------------ 注册

    @Transactional
    public Account register(AccountDtos.RegisterRequest body, HttpServletRequest request,
                            HttpServletResponse response) {
        String ip = RequestUtils.clientIp(request);
        rateLimiter.check("register:ip:" + ip, REGISTER_LIMIT_PER_IP, REGISTER_IP_WINDOW);

        if (!body.agree()) {
            throw AccountException.badRequest("AGREE_REQUIRED", "请先阅读并同意服务条款与隐私政策");
        }
        Account.ContactType type = parseContactType(body.contactType());
        ContactNormalizer.validate(type, body.contactValue());
        String contactNorm = ContactNormalizer.normalize(type, body.contactValue());
        PasswordPolicy.validate(body.password(), body.contactValue());

        String displayName = body.displayName() == null ? "" : body.displayName().trim();
        if (displayName.isEmpty()) {
            throw AccountException.badRequest("INVALID_DISPLAY_NAME", "请填写昵称");
        }

        // 注册接口必须明确告知「该联系方式已注册」——否则用户无法判断该去登录还是换一个，
        // 体验损失大于枚举风险。枚举风险由限流与后续的登录保护承担。
        if (accounts.existsByContactTypeAndContactNorm(type, contactNorm)) {
            throw AccountException.conflict("CONTACT_TAKEN", "该联系方式已注册，请直接登录");
        }
        emailService.consumeRegisterCode(body.contactValue(), body.code());

        Account account = Account.builder()
                .contactType(type)
                .contactValue(body.contactValue().trim())
                .contactNorm(contactNorm)
                .contactVerified(true)
                .publicUserId(generatePublicUserId())
                .email(body.contactValue().trim())
                .emailNorm(contactNorm)
                .emailVerified(true)
                .passwordHash(encoder.encode(body.password()))
                .displayName(displayName)
                .role("BUYER")
                .levelCode("NEW")
                .status(Account.AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();
        account = accounts.save(account);

        audit.record(account.getId(), "REGISTER", "type=" + type.name(), request);
        sessionService.issue(account, request, response, false);
        return account;
    }

    // ------------------------------------------------------------------ 登录

    @Transactional
    public Account login(AccountDtos.LoginRequest body, HttpServletRequest request,
                         HttpServletResponse response) {
        String ip = RequestUtils.clientIp(request);
        rateLimiter.check("login:ip:" + ip, LOGIN_LIMIT_PER_IP, LOGIN_IP_WINDOW);

        Account.ContactType type = parseContactType(body.contactType());
        String contactNorm = ContactNormalizer.normalize(type, body.contactValue());
        rateLimiter.check("login:acct:" + type.name() + ":" + contactNorm,
                AccountLockService.MAX_FAILED_ATTEMPTS * 3, AccountLockService.LOCK_DURATION);

        // 下面所有失败分支都通过 lockService 记录：它是 REQUIRES_NEW 独立事务，
        // 因为这些分支紧接着就会抛异常，而本方法是 @Transactional ——
        // 写在本事务里的失败计数与审计会被回滚掉（修复前的实际缺陷）。
        Optional<Account> found = accounts.findByContactTypeAndContactNorm(type, contactNorm);
        if (found.isEmpty()) {
            // 与「密码错误」返回完全相同的错误，并额外跑一次 BCrypt 把耗时拉平，
            // 否则响应时间本身就是账号是否存在的判据。
            encoder.matches(body.password(), timingDecoyHash);
            lockService.recordFailure(null, "NO_ACCOUNT", request);
            throw invalidCredentials();
        }
        Account account = found.get();

        if (!account.isLoginable()) {
            lockService.recordFailure(account.getId(), "STATUS_" + account.getStatus(), request);
            throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已被停用，请联系客服");
        }
        if (account.isLockedNow()) {
            lockService.recordFailure(account.getId(), "LOCKED", request);
            throw AccountException.forbidden("ACCOUNT_LOCKED", "登录失败次数过多，请稍后再试");
        }
        if (!encoder.matches(body.password(), account.getPasswordHash())) {
            lockService.recordFailure(account.getId(), "BAD_PASSWORD", request);
            throw invalidCredentials();
        }

        lockService.applySuccess(account, ip);

        audit.record(account.getId(), "LOGIN_OK", null, request);
        sessionService.issue(account, request, response, body.remember());
        return account;
    }

    private AccountException invalidCredentials() {
        return AccountException.unauthorized("INVALID_CREDENTIALS", "账号或密码不正确");
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

    /** 当前账号体系只允许邮箱注册与邮箱登录。 */
    static Account.ContactType parseContactType(String raw) {
        String value = raw == null || raw.isBlank() ? "EMAIL" : raw.trim().toUpperCase(Locale.ROOT);
        if (!"EMAIL".equals(value)) {
            throw AccountException.badRequest("EMAIL_ONLY", "当前仅支持邮箱注册和邮箱登录");
        }
        return Account.ContactType.EMAIL;
    }

    // ------------------------------------------------------------------ 登出

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessionService.currentSessionId(request).ifPresent(sid -> {
            sessionService.revoke(sid);
            audit.record(null, "LOGOUT", null, request);
        });
        sessionService.clearCookie(response);
    }

    // ------------------------------------------------------------------ 改密

    @Transactional
    public void changePassword(Account account, AccountDtos.ChangePasswordRequest body,
                               HttpServletRequest request, HttpServletResponse response) {
        if (!encoder.matches(body.oldPassword(), account.getPasswordHash())) {
            audit.record(account.getId(), "PWD_CHANGE_FAIL", "reason=old_password_mismatch", request);
            throw AccountException.badRequest("OLD_PASSWORD_MISMATCH", "原密码不正确");
        }
        PasswordPolicy.validate(body.newPassword(), account.getContactValue());
        if (encoder.matches(body.newPassword(), account.getPasswordHash())) {
            throw AccountException.badRequest("SAME_PASSWORD", "新密码不能与原密码相同");
        }
        account.setPasswordHash(encoder.encode(body.newPassword()));
        accounts.save(account);

        // 改密后必须让所有其他设备下线 —— 否则密码泄露后改密并不能把攻击者踢出去。
        sessionService.revokeAll(account.getId());
        sessionService.clearCookie(response);
        audit.record(account.getId(), "PWD_CHANGE", null, request);
    }
}
