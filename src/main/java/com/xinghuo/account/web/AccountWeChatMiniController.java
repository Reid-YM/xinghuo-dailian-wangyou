package com.xinghuo.account.web;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.AccountSessionService;
import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.account.service.WeChatMiniBridgeService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序登录网站：浏览器领取短期码，小程序确认，浏览器领取本站会话。 */
@RestController
@RequestMapping("/api/account/wechat-mini")
public class AccountWeChatMiniController {
    private static final String COOKIE = "XH_MINI_PAIR";
    private final WeChatMiniBridgeService bridge;
    private final AccountSessionService sessions;
    private final RateLimiter rateLimiter;
    private final boolean secureCookie;

    public AccountWeChatMiniController(WeChatMiniBridgeService bridge,
                                       AccountSessionService sessions,
                                       RateLimiter rateLimiter,
                                       @Value("${app.base-url:}") String baseUrl) {
        this.bridge = bridge;
        this.sessions = sessions;
        this.rateLimiter = rateLimiter;
        this.secureCookie = baseUrl != null && baseUrl.startsWith("https://");
    }

    @GetMapping("/availability")
    public Availability availability() {
        return new Availability(bridge.configured());
    }

    @PostMapping("/challenge")
    public ChallengeView challenge(HttpServletRequest request, HttpServletResponse response) {
        rateLimiter.check("mini:challenge:" + RequestUtils.clientIp(request), 10, Duration.ofMinutes(15));
        Account current = sessions.current(request).orElse(null);
        WeChatMiniBridgeService.Challenge issued = bridge.issue(current == null ? null : current.getId());
        response.addHeader("Set-Cookie", cookie(issued.browserSecret(), 300));
        return new ChallengeView(issued.code(), issued.expiresIn(), issued.binding());
    }

    @PostMapping("/poll")
    public PollView poll(HttpServletRequest request, HttpServletResponse response) {
        rateLimiter.check("mini:poll:" + RequestUtils.clientIp(request), 150, Duration.ofMinutes(5));
        WeChatMiniBridgeService.PollResult result = bridge.poll(readCookie(request));
        if ("expired".equals(result.status())) {
            response.addHeader("Set-Cookie", cookie("", 0));
        }
        return new PollView(result.status(), result.identityHint());
    }

    @PostMapping("/complete")
    public PollView complete(HttpServletRequest request, HttpServletResponse response) {
        rateLimiter.check("mini:complete:" + RequestUtils.clientIp(request), 10, Duration.ofMinutes(15));
        Long currentId = sessions.current(request).map(Account::getId).orElse(null);
        Account account = bridge.complete(readCookie(request), currentId);
        sessions.issue(account, request, response, false);
        response.addHeader("Set-Cookie", cookie("", 0));
        return new PollView("approved", "");
    }

    @PostMapping("/profile")
    public WeChatMiniBridgeService.MiniProfile profile(@Valid @RequestBody LoginCode body,
                                                        HttpServletRequest request) {
        rateLimiter.check("mini:profile:" + RequestUtils.clientIp(request), 30, Duration.ofMinutes(15));
        return bridge.profile(body.loginCode());
    }

    @PostMapping("/confirm")
    public WeChatMiniBridgeService.MiniProfile confirm(@Valid @RequestBody Confirm body,
                                                        HttpServletRequest request) {
        rateLimiter.check("mini:confirm:ip:" + RequestUtils.clientIp(request), 15, Duration.ofMinutes(15));
        rateLimiter.check("mini:confirm:code:" + body.code(), 5, Duration.ofMinutes(5));
        return bridge.confirm(body.code(), body.loginCode());
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String cookie(String value, int maxAge) {
        return COOKIE + "=" + value + "; Path=/api/account/wechat-mini; Max-Age=" + maxAge
                + "; HttpOnly; SameSite=Lax" + (secureCookie ? "; Secure" : "");
    }

    public record Availability(boolean enabled) {}
    public record ChallengeView(String code, long expiresIn, boolean binding) {}
    public record PollView(String status, String identityHint) {}
    public record LoginCode(@NotBlank String loginCode) {}
    public record Confirm(@NotBlank String code, @NotBlank String loginCode) {}
}
