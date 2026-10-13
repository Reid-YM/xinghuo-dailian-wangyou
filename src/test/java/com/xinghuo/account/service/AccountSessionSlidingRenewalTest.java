package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountSession;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.repo.AccountSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * 会话滑动续期（2026-10-13）：
 * expires_at 从「签发时间 + 固定 TTL」改成「空闲截止时间 + 绝对上限」。
 *
 * <p>这几条用例锁住四件事：
 * <ol>
 *   <li>一直在用的会话会被续期，不再固定 8 小时踢下线；</li>
 *   <li>续期绝不越过绝对上限；</li>
 *   <li>升级前的老会话（没有 idle_seconds / absolute_expires_at）行为不变；</li>
 *   <li>Cookie 的 Max-Age 按绝对上限下发，否则浏览器会先于会话把 Cookie 丢掉。</li>
 * </ol>
 */
class AccountSessionSlidingRenewalTest {

    private static final Duration IDLE = Duration.ofHours(8);

    private final AccountSessionRepository sessions = mock(AccountSessionRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountSessionService service =
            new AccountSessionService(sessions, accounts, "", "http://localhost");

    @Test
    void activeSessionIsRenewedInsteadOfExpiringAtFixedEightHours() {
        Instant now = Instant.now();
        // 8 小时前签发、10 分钟后就要到期（旧行为下用户马上被踢），但刚刚访问过。
        AccountSession session = slidingSession(now, now.plus(Duration.ofMinutes(10)), now.plus(Duration.ofDays(30)));
        session.setLastSeenAt(now.minus(Duration.ofHours(1)));
        stubSession(session);

        assertTrue(service.current(request("sid")).isPresent(), "会话应当仍然可用");

        Instant renewed = capturedDeadline();
        assertTrue(renewed.isAfter(now.plus(IDLE).minus(Duration.ofMinutes(1))),
                "活跃会话应被推到 now + 8 小时，实际 " + renewed);
        assertTrue(renewed.isBefore(now.plus(IDLE).plus(Duration.ofMinutes(1))),
                "续期不应超过空闲窗口，实际 " + renewed);
    }

    @Test
    void renewalNeverPassesTheAbsoluteCap() {
        Instant now = Instant.now();
        AccountSession session = slidingSession(now, now.plus(Duration.ofMinutes(10)), now.plus(Duration.ofMinutes(30)));
        session.setLastSeenAt(now.minus(Duration.ofHours(1)));
        stubSession(session);

        service.current(request("sid"));

        assertEquals(session.getAbsoluteExpiresAt(), capturedDeadline(),
                "空闲窗口再长也不能越过绝对上限");
    }

    @Test
    void legacySessionWithoutSlidingColumnsKeepsItsOriginalDeadline() {
        Instant now = Instant.now();
        // 升级前签发的老会话：只有固定到期时间，没有空闲窗口与绝对上限。
        AccountSession legacy = AccountSession.builder()
                .id("sid")
                .userId(7L)
                .createdAt(now.minus(Duration.ofHours(7)))
                .lastSeenAt(now.minus(Duration.ofHours(1)))
                .expiresAt(now.plus(Duration.ofMinutes(20)))
                .build();
        stubSession(legacy);

        service.current(request("sid"));

        assertEquals(legacy.getExpiresAt(), capturedDeadline(),
                "老会话不得因为本次上线被延长");
    }

    @Test
    void freshRequestsDoNotWriteDatabase() {
        Instant now = Instant.now();
        AccountSession session = slidingSession(now, now.plus(Duration.ofMinutes(10)), now.plus(Duration.ofDays(30)));
        session.setLastSeenAt(now.minus(Duration.ofMinutes(1)));
        stubSession(session);

        service.current(request("sid"));

        verify(sessions, never()).touch(anyString(), any(), any(), any());
    }

    @Test
    void issuedSessionCarriesSlidingColumnsAndCookieOutlivesIdleWindow() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        AccountSession session = service.issue(account(), request(null), response, false);

        assertEquals((int) IDLE.toSeconds(), (int) session.getIdleSeconds(),
                "签发时必须写入空闲窗口，否则这条会话永远不会被续期");
        assertNotNull(session.getAbsoluteExpiresAt(), "签发时必须写入绝对上限");
        assertTrue(session.getAbsoluteExpiresAt().isAfter(session.getExpiresAt()),
                "绝对上限必须晚于空闲截止时间，否则滑动续期没有空间");

        ArgumentCaptor<String> cookie = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), cookie.capture());
        int maxAge = Integer.parseInt(cookie.getValue().replaceAll(".*Max-Age=(\\d+).*", "$1"));
        assertTrue(maxAge >= Duration.ofDays(30).toSeconds(),
                "Cookie Max-Age 必须覆盖绝对上限，否则浏览器先丢 Cookie，滑动续期形同虚设");
    }

    // ------------------------------------------------------------------ 脚手架

    private static AccountSession slidingSession(Instant now, Instant expiresAt, Instant absolute) {
        return AccountSession.builder()
                .id("sid")
                .userId(7L)
                .createdAt(now.minus(IDLE))
                .lastSeenAt(now.minus(Duration.ofHours(7)))
                .expiresAt(expiresAt)
                .idleSeconds((int) IDLE.toSeconds())
                .absoluteExpiresAt(absolute)
                .build();
    }

    private static Account account() {
        return Account.builder().id(7L).status(Account.AccountStatus.ACTIVE).build();
    }

    private static HttpServletRequest request(String sid) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        if (sid != null) {
            when(request.getCookies()).thenReturn(new Cookie[] {new Cookie("XH_SID", sid)});
        }
        return request;
    }

    private void stubSession(AccountSession session) {
        when(sessions.findByIdAndRevokedAtIsNull(session.getId())).thenReturn(Optional.of(session));
        when(accounts.findById(session.getUserId())).thenReturn(Optional.of(account()));
    }

    private Instant capturedDeadline() {
        ArgumentCaptor<Instant> deadline = ArgumentCaptor.forClass(Instant.class);
        verify(sessions).touch(eq("sid"), any(), any(), deadline.capture());
        return deadline.getValue();
    }
}