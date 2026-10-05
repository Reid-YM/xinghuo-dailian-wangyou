package com.xinghuo.account.service;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountSession;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.repo.AccountSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录会话：签发、解析、吊销。
 *
 * <p>为什么不用 HttpSession：蓝绿发布会起新 JVM，内存里的登录态全部丢失，
 * 每次发布都让全站用户掉线。会话落到 user_session 表后，发布不影响登录态，
 * 并且天然获得「我的设备」与远程下线能力。
 *
 * <p>为什么不用 spring-session-jdbc：基线 jar 的依赖是固定的，构建脚本只编译
 * 覆盖源码、不新增 Maven 依赖，因此无法引入新 jar。
 */
@Service
public class AccountSessionService {

    /** Cookie 名。注意：Cookie 里只有随机会话 id，不含任何用户信息。 */
    public static final String COOKIE_NAME = "XH_SID";

    private static final Duration DEFAULT_TTL = Duration.ofHours(8);
    private static final Duration REMEMBER_TTL = Duration.ofDays(30);

    /**
     * last_seen_at 的刷新阈值。
     *
     * <p>会话解析发生在每个请求上，无条件写库会把每次页面访问都变成一次 UPDATE。
     * 5 分钟粒度对「最后活跃时间」的展示完全够用。
     */
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(5);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountSessionRepository sessions;
    private final AccountRepository accounts;
    private final boolean cookieSecure;

    /**
     * 会话 Cookie 是否带 {@code Secure} 位。
     *
     * <p>修复 B-1：此前只读 {@code server.servlet.session.cookie.secure}，
     * 生产若忘了显式配置为 {@code true}，会话 Cookie 就会在非 HTTPS（或 HTTPS
     * 降级）连接上明文传输，被中间人截获即可直接窃取登录态。现在只要站点
     * {@code app.base-url} 是 {@code https://}（生产由 Nginx 终结 TLS、本站实际
     * 是 http 到后端，因此用 base-url 判断），Secure 位就强制为 true，避免「忘了
     * 配就裸奔」。本地开发 base-url 为 http，仍需配置为 true 才生效（见 .env.example
     * 注释），不会波及开发联调。
     */
    private static boolean resolveCookieSecure(String configured, String baseUrl) {
        if ("true".equalsIgnoreCase(configured == null ? "" : configured)) {
            return true;
        }
        return baseUrl != null && baseUrl.startsWith("https://");
    }

    public AccountSessionService(AccountSessionRepository sessions,
                                 AccountRepository accounts,
                                 @Value("${server.servlet.session.cookie.secure:}") String cookieSecureConfig,
                                 @Value("${app.base-url:}") String baseUrl) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.cookieSecure = resolveCookieSecure(cookieSecureConfig, baseUrl);
    }

    // ------------------------------------------------------------------ 解析

    /** 取当前登录账号。未登录、会话失效、账号被禁用都返回 empty。 */
    @Transactional
    public Optional<Account> current(HttpServletRequest request) {
        String sid = readCookie(request);
        if (sid == null) {
            return Optional.empty();
        }
        Optional<AccountSession> found = sessions.findByIdAndRevokedAtIsNull(sid);
        if (found.isEmpty() || !found.get().isUsable()) {
            return Optional.empty();
        }
        AccountSession session = found.get();
        Optional<Account> account = accounts.findById(session.getUserId());
        if (account.isEmpty() || !account.get().isLoginable()) {
            return Optional.empty();
        }
        touchIfStale(session);
        return account;
    }

    /** 取当前会话 id（用于在设备列表里标记「本机」）。 */
    public Optional<String> currentSessionId(HttpServletRequest request) {
        return Optional.ofNullable(readCookie(request));
    }

    private void touchIfStale(AccountSession session) {
        Instant now = Instant.now();
        if (session.getLastSeenAt() != null
                && session.getLastSeenAt().isAfter(now.minus(TOUCH_INTERVAL))) {
            return;
        }
        sessions.touch(session.getId(), now, now.minus(TOUCH_INTERVAL));
    }

    private String readCookie(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                String v = c.getValue();
                return v == null || v.isBlank() ? null : v;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 签发

    /**
     * 签发新会话并写入 Cookie。
     *
     * <p>会话 id 用 SecureRandom 生成 32 字节再 Base64 编码，而不是 UUID：
     * 会话 id 是「持有即可登录」的凭据，熵越高越好。
     */
    public AccountSession issue(Account account, HttpServletRequest request,
                                HttpServletResponse response, boolean remember) {
        Instant now = Instant.now();
        String id = newSessionId();
        AccountSession session = AccountSession.builder()
                .id(id)
                .userId(account.getId())
                .ip(RequestUtils.clientIp(request))
                .userAgent(RequestUtils.userAgent(request))
                .deviceLabel(RequestUtils.deviceLabel(RequestUtils.userAgent(request)))
                .createdAt(now)
                .lastSeenAt(now)
                .expiresAt(now.plus(remember ? REMEMBER_TTL : DEFAULT_TTL))
                .build();
        sessions.save(session);
        writeCookie(response, id, (int) (remember ? REMEMBER_TTL : DEFAULT_TTL).toSeconds());
        return session;
    }

    private String newSessionId() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ------------------------------------------------------------------ 吊销

    @Transactional
    public void revoke(String sessionId) {
        sessions.findById(sessionId).ifPresent(s -> {
            s.setRevokedAt(Instant.now());
            sessions.save(s);
        });
    }

    /** 吊销某用户的全部会话。改密、重置密码后必须调用。 */
    @Transactional
    public int revokeAll(Long userId) {
        return sessions.revokeAllForUser(userId, Instant.now());
    }

    /** 吊销除当前会话外的其他会话。 */
    @Transactional
    public int revokeOthers(Long userId, String keepSessionId) {
        return sessions.revokeOthers(userId, keepSessionId, Instant.now());
    }

    @Transactional
    public List<AccountSession> listActive(Long userId) {
        return sessions.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(
                userId, Instant.now());
    }

    /** 清除浏览器 Cookie。注意要带同样的 Path 与 SameSite，否则某些浏览器不会覆盖。 */
    public void clearCookie(HttpServletResponse response) {
        writeCookie(response, "", 0);
    }

    private void writeCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        StringBuilder sb = new StringBuilder();
        sb.append(COOKIE_NAME).append('=').append(value)
                .append("; Path=/")
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; HttpOnly")
                .append("; SameSite=Lax");
        if (cookieSecure) {
            sb.append("; Secure");
        }
        // Servlet API 的 Cookie 类不支持 SameSite，只能手写 Set-Cookie 头。
        response.addHeader("Set-Cookie", sb.toString());
    }

    /** 会话 id 是否形如本服务签发的格式，避免把任意 Cookie 值拿去查库。 */
    public static boolean looksLikeSessionId(String value) {
        return value != null && value.length() >= 32 && value.length() <= 64
                && value.chars().allMatch(c -> c == '-' || c == '_'
                        || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }
}
