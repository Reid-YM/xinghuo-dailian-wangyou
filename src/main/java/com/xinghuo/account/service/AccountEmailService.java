package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountToken;
import com.xinghuo.account.repo.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 邮箱验证与找回密码。
 *
 * <p>两条防枚举规则：
 * <ul>
 *   <li>「发送重置邮件」无论邮箱是否存在都返回同样的成功响应；</li>
 *   <li>「验证邮箱」失败时不区分「令牌不存在」「已使用」「已过期」。</li>
 * </ul>
 * 否则接口会变成一个「这个邮箱是否注册过本站」的查询器。
 */
@Service
public class AccountEmailService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private static final int VERIFY_LIMIT_PER_ACCOUNT = 3;
    private static final int REGISTER_LIMIT_PER_EMAIL = 3;
    private static final int RESET_LIMIT_PER_EMAIL = 3;
    private static final int LIMIT_PER_IP = 10;
    private static final Duration HOUR = Duration.ofHours(1);

    private final AccountRepository accounts;
    private final AccountTokenService tokenService;
    private final AccountSessionService sessionService;
    private final SmtpMailSender mail;
    private final PasswordEncoder encoder;
    private final AccountAuditService audit;
    private final RateLimiter rateLimiter;
    private final String baseUrl;

    public AccountEmailService(AccountRepository accounts,
                               AccountTokenService tokenService,
                               AccountSessionService sessionService,
                               SmtpMailSender mail,
                               PasswordEncoder encoder,
                               AccountAuditService audit,
                               RateLimiter rateLimiter,
                               @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.accounts = accounts;
        this.tokenService = tokenService;
        this.sessionService = sessionService;
        this.mail = mail;
        this.encoder = encoder;
        this.audit = audit;
        this.rateLimiter = rateLimiter;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    // -------------------------------------------------------------- 注册邮箱验证码

    /** 发送注册邮箱验证码（公开接口）。 */
    @Transactional
    public void requestRegisterCode(String rawEmail, HttpServletRequest request) {
        if (!mail.isConfigured()) {
            throw AccountException.serverError("MAIL_NOT_CONFIGURED",
                    "邮箱服务尚未配置，暂时无法注册账号");
        }
        ContactNormalizer.validateEmail(rawEmail);
        String norm = ContactNormalizer.normalizeEmail(rawEmail);
        String ip = RequestUtils.clientIp(request);
        rateLimiter.check("email:register:email:" + norm, REGISTER_LIMIT_PER_EMAIL, HOUR);
        rateLimiter.check("email:register:ip:" + ip, LIMIT_PER_IP, HOUR);
        if (accounts.existsByContactTypeAndContactNorm(Account.ContactType.EMAIL, norm)
                || accounts.findByEmailNorm(norm).isPresent()) {
            throw AccountException.conflict("CONTACT_TAKEN", "该邮箱已注册，请直接登录");
        }

        String code = tokenService.issueCode(null, AccountToken.Purpose.REGISTER_EMAIL,
                registerScope(norm), TOKEN_TTL, request);
        mail.send(rawEmail.trim(), "星火网游 · 注册验证码",
                "你好：\n\n"
                        + "你的注册验证码是：" + code + "\n"
                        + "验证码 30 分钟内有效，用后即失效。\n\n"
                        + "如果这不是你本人的操作，忽略本邮件即可。\n");
        audit.record(null, "REGISTER_EMAIL_CODE_SENT", "email=" + norm, request);
    }

    /** 校验并消费注册邮箱验证码。 */
    @Transactional
    public void consumeRegisterCode(String rawEmail, String code) {
        ContactNormalizer.validateEmail(rawEmail);
        String norm = ContactNormalizer.normalizeEmail(rawEmail);
        tokenService.consumeCode(registerScope(norm), code, AccountToken.Purpose.REGISTER_EMAIL)
                .orElseThrow(() -> AccountException.badRequest("CODE_INVALID",
                        "验证码无效或已过期，请重新发送"));
    }

    // -------------------------------------------------------------- 邮箱验证

    /**
     * 绑定并发送验证邮件。
     *
     * <p>邮箱在验证通过前先写入账号（标记为未验证），这样用户在设置页能看到
     * 「待验证」状态并重新发送，而不是每次都要重新输入。
     */
    @Transactional
    public void requestVerify(Account account, String rawEmail, HttpServletRequest request) {
        if (!mail.isConfigured()) {
            throw AccountException.serverError("MAIL_NOT_CONFIGURED",
                    "邮箱服务尚未配置，暂时无法绑定邮箱");
        }
        ContactNormalizer.validateEmail(rawEmail);
        String norm = ContactNormalizer.normalizeEmail(rawEmail);
        String ip = RequestUtils.clientIp(request);
        rateLimiter.check("email:verify:acct:" + account.getId(),
                VERIFY_LIMIT_PER_ACCOUNT, HOUR);
        rateLimiter.check("email:verify:ip:" + ip, LIMIT_PER_IP, HOUR);

        if (norm != null && !norm.equals(account.getEmailNorm())) {
            Optional<Account> other = accounts.findByEmailNorm(norm);
            if (other.isPresent() && !other.get().getId().equals(account.getId())) {
                throw AccountException.conflict("EMAIL_TAKEN", "该邮箱已被其他账号绑定");
            }
        }

        boolean changed = norm != null && !norm.equals(account.getEmailNorm());
        account.setEmail(rawEmail.trim());
        account.setEmailNorm(norm);
        if (changed) {
            account.setEmailVerified(false);
        }
        accounts.save(account);

        String code = tokenService.issueCode(account.getId(), AccountToken.Purpose.VERIFY_EMAIL,
                verifyScope(account), TOKEN_TTL, request);
        mail.send(account.getEmail(), "星火网游 · 邮箱验证码",
                "你好 " + account.getDisplayName() + "：\n\n"
                        + "你的邮箱验证码是：" + code + "\n"
                        + "验证码 30 分钟内有效，用后即失效。\n\n"
                        + "如果这不是你本人的操作，忽略本邮件即可。\n");
        audit.record(account.getId(), "EMAIL_VERIFY_SENT", null, request);
    }

    @Transactional
    public void verifyEmailCode(Account account, String code, HttpServletRequest request) {
        AccountToken consumed = tokenService
                .consumeCode(verifyScope(account), code, AccountToken.Purpose.VERIFY_EMAIL)
                .orElseThrow(() -> AccountException.badRequest("CODE_INVALID",
                        "验证码无效或已过期，请重新发送"));
        if (!account.getId().equals(consumed.getUserId())) {
            throw AccountException.badRequest("CODE_INVALID", "验证码无效或已过期，请重新发送");
        }
        account.setEmailVerified(true);
        account.setContactVerified(true);
        account.setContactType(Account.ContactType.EMAIL);
        account.setContactValue(account.getEmail());
        account.setContactNorm(account.getEmailNorm());
        accounts.save(account);
        audit.record(account.getId(), "EMAIL_VERIFIED", null, request);
    }

    @Transactional
    public void verifyEmail(String token, HttpServletRequest request) {
        AccountToken consumed = tokenService
                .consume(token, AccountToken.Purpose.VERIFY_EMAIL)
                .orElseThrow(() -> AccountException.badRequest("TOKEN_INVALID",
                        "验证链接无效或已过期，请重新发送"));
        Account account = accounts.findById(consumed.getUserId())
                .orElseThrow(() -> AccountException.badRequest("TOKEN_INVALID",
                        "验证链接无效或已过期，请重新发送"));
        account.setEmailVerified(true);
        accounts.save(account);
        audit.record(account.getId(), "EMAIL_VERIFIED", null, request);
    }

    // -------------------------------------------------------------- 找回密码

    /**
     * 发送重置邮件。
     *
     * <p>无论邮箱是否存在、是否已验证，都返回同样的成功结果。
     */
    @Transactional
    public void requestPasswordReset(String rawEmail, HttpServletRequest request) {
        ContactNormalizer.validateEmail(rawEmail);
        String norm = ContactNormalizer.normalizeEmail(rawEmail);
        String ip = RequestUtils.clientIp(request);
        rateLimiter.check("pwd:reset:email:" + norm, RESET_LIMIT_PER_EMAIL, HOUR);
        rateLimiter.check("pwd:reset:ip:" + ip, LIMIT_PER_IP, HOUR);

        // 「邮件未配置」必须先于账号查找判定。
        //
        // 曾经把这段放在 findByEmailNorm 之后，于是生产环境（MAIL_* 未配置）下
        // 形成一条干净的账号枚举 oracle：已注册邮箱走到这里抛 502，
        // 未注册邮箱在上一行就 return 200 —— 一行 curl 比较状态码即可批量标注邮箱。
        // 前提判定放在前面后，两种情况返回完全一致的结果。
        if (!mail.isConfigured()) {
            throw AccountException.serverError("MAIL_NOT_CONFIGURED",
                    "邮箱服务尚未配置，请通过客服渠道找回密码");
        }

        Optional<Account> found = accounts.findByEmailNorm(norm);
        if (found.isEmpty() || !found.get().isLoginable()) {
            audit.record(null, "PWD_RESET_REQUEST", "reason=no_match", request);
            return;
        }
        Account account = found.get();

        String code = tokenService.issueCode(account.getId(), AccountToken.Purpose.RESET_PASSWORD,
                resetScope(norm), TOKEN_TTL, request);
        mail.send(account.getEmail(), "星火网游 · 重置密码验证码",
                "你好 " + account.getDisplayName() + "：\n\n"
                        + "你的重置密码验证码是：" + code + "\n"
                        + "验证码 30 分钟内有效，用后即失效。\n\n"
                        + "如果这不是你本人的操作，请尽快联系客服。\n");
        audit.record(account.getId(), "PWD_RESET_SENT", null, request);
    }

    @Transactional
    public void resetPasswordByCode(String rawEmail, String code, String newPassword, HttpServletRequest request) {
        ContactNormalizer.validateEmail(rawEmail);
        String norm = ContactNormalizer.normalizeEmail(rawEmail);
        AccountToken consumed = tokenService
                .consumeCode(resetScope(norm), code, AccountToken.Purpose.RESET_PASSWORD)
                .orElseThrow(() -> AccountException.badRequest("CODE_INVALID",
                        "验证码无效或已过期，请重新申请"));
        Account account = accounts.findById(consumed.getUserId())
                .orElseThrow(() -> AccountException.badRequest("CODE_INVALID",
                        "验证码无效或已过期，请重新申请"));
        resetPasswordForAccount(account, newPassword, request);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, HttpServletRequest request) {
        AccountToken consumed = tokenService
                .consume(token, AccountToken.Purpose.RESET_PASSWORD)
                .orElseThrow(() -> AccountException.badRequest("TOKEN_INVALID",
                        "重置链接无效或已过期，请重新申请"));
        Account account = accounts.findById(consumed.getUserId())
                .orElseThrow(() -> AccountException.badRequest("TOKEN_INVALID",
                        "重置链接无效或已过期，请重新申请"));
        resetPasswordForAccount(account, newPassword, request);
    }

    private void resetPasswordForAccount(Account account, String newPassword, HttpServletRequest request) {
        if (!account.isLoginable()) {
            throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已被停用，请联系客服");
        }
        PasswordPolicy.validate(newPassword, account.getEmail() == null ? account.getContactValue() : account.getEmail());
        account.setPasswordHash(encoder.encode(newPassword));
        account.setEmailVerified(true);
        account.setContactVerified(true);
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        accounts.save(account);
        sessionService.revokeAll(account.getId());
        audit.record(account.getId(), "PWD_RESET", null, request);
    }

    private static String verifyScope(Account account) {
        return "verify:" + account.getId() + ":" + (account.getEmailNorm() == null ? "" : account.getEmailNorm());
    }

    private static String resetScope(String emailNorm) {
        return "reset:" + emailNorm;
    }

    private static String registerScope(String emailNorm) {
        return "register:" + emailNorm;
    }
}
