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
 *
 * <p>过期语义：{@code expires_at} 是空闲截止时间 —— 每次活跃由 {@link #touchIfStale}
 * 推到「now + 空闲窗口」，所以一直在用的人不会被固定 8 小时强行踢下线；
 * {@code absolute_expires_at} 是硬上限，防止被盗 Cookie 靠定时刷新续成永久有效。
 * 升级前签发的老会话这两列为空，保持原来的固定到期行为（不会因为本次上线被延长）。
 */
@Service
public class AccountSessionService {

    /** Cookie 名。注意：Cookie 里只有随机会话 id，不含任何用户信息。 */
    public static final String COOKIE_NAME = "XH_SID";

    /** 空闲窗口：距上次活跃超过它就失效（未勾「记住这台设备」）。 */
    private static final Duration IDLE_TTL = Duration.ofHours(8);
    /** 空闲窗口：勾了「记住这台设备」时 30 天不用才失效。 */
    private static final Duration IDLE_TTL_REMEMBER = Duration.ofDays(30);

    /**
     * 绝对上限：无论多活跃，到点必须重新登录。
     *
     * <p>滑动续期解决「一直在用也被 8 小时踢掉」，但会话 id 是持有即可登录的凭据，
     * 不能被一条定时刷新的脚本续成永久有效，所以再压一道硬上限。
     */
    private static final Duration MAX_TTL = Duration.ofDays(30);
    private static final Duration MAX_TTL_REMEMBER = Duration.ofDays(180);

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

    /**
     * 活跃就打点：刷新 last_seen_at，并把空闲截止时间滑动到 now + 空闲窗口。
     *
     * <p>仍然按 TOUCH_INTERVAL 节流，所以「滑动」是分钟级粒度 —— 对 8 小时的空闲窗口
     * 完全够用，也不会把每次页面访问都变成一次 UPDATE。
     */
    private void touchIfStale(AccountSession session) {
        Instant now = Instant.now();
        if (session.getLastSeenAt() != null
                && session.getLastSeenAt().isAfter(now.minus(TOUCH_INTERVAL))) {
            return;
        }
        sessions.touch(session.getId(), now, now.minus(TOUCH_INTERVAL), renewedDeadline(session, now));
    }

    /**
     * 续期后的空闲截止时间：now + 空闲窗口，并截断在绝对上限之内。
     *
     * <p>两种情况干脆沿用原到期时间（等于不续期）：升级前的老会话（两列为空），
     * 以及已经顶到绝对上限的会话。
     */
    private static Instant renewedDeadline(AccountSession session, Instant now) {
        Instant current = session.getExpiresAt();
        Integer idleSeconds = session.getIdleSeconds();
        Instant absolute = session.getAbsoluteExpiresAt();
        if (idleSeconds == null || idleSeconds <= 0 || absolute == null) {
            return current;
        }
        Instant next = now.plusSeconds(idleSeconds);
        if (current != null && next.isBefore(current)) {
            next = current; // 永不缩短：并发请求可能已经把截止时间推得更远
        }
        return next.isAfter(absolute) ? absolute : next;
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
     *
     * <p>签发后过期时间是滑动的：remember 只决定空闲窗口（8 小时 / 30 天），
     * 绝对上限（30 天 / 180 天）两条路一样硬。
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
                .expiresAt(now.plus(idleTtl(remember)))
                .idleSeconds((int) idleTtl(remember).toSeconds())
                .absoluteExpiresAt(now.plus(maxTtl(remember)))
                .build();
        sessions.save(session);
        // Cookie 的 Max-Age 取绝对上限而不是空闲窗口：浏览器只在写入时记这个时间，之后不会随请求刷新。
        // 若按空闲窗口写 8 小时，滑动续期到第 8 小时反而会被浏览器自己丢掉 Cookie。
        // 「多久不用就失效」由库里的 expires_at 判定，Cookie 活得久一点不会给未登录请求任何权限。
        writeCookie(response, id, (int) maxTtl(remember).toSeconds());
        return session;
    }

    private static Duration idleTtl(boolean remember) {
        return remember ? IDLE_TTL_REMEMBER : IDLE_TTL;
    }

    private static Duration maxTtl(boolean remember) {
        return remember ? MAX_TTL_REMEMBER : MAX_TTL;
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
