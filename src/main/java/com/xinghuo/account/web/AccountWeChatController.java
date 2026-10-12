package com.xinghuo.account.web;

import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.account.service.WeChatLoginService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 微信开放平台网站应用登录入口与回调。 */
@Controller
@RequestMapping("/api/account/wechat")
public class AccountWeChatController {

    private static final String FLOW_COOKIE = "XH_WX_OAUTH";
    private static final int FLOW_TTL_SECONDS = 10 * 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WeChatLoginService weChatLogin;
    private final RateLimiter rateLimiter;
    private final boolean secureCookie;

    public AccountWeChatController(WeChatLoginService weChatLogin,
                                   RateLimiter rateLimiter,
                                   @Value("${app.base-url:}") String baseUrl) {
        this.weChatLogin = weChatLogin;
        this.rateLimiter = rateLimiter;
        this.secureCookie = baseUrl != null && baseUrl.trim().startsWith("https://");
    }

    @GetMapping("/availability")
    @ResponseBody
    public java.util.Map<String, Boolean> availability() {
        return java.util.Map.of("enabled", weChatLogin.configured());
    }

    /**
     * 发起扫码登录。next 只允许站内相对路径，并与随机 state 一起放入 HttpOnly 短期 Cookie。
     * 这样蓝绿发布时不依赖 JVM HttpSession，也不会把 AppSecret 或用户身份放到浏览器。
     */
    @GetMapping("/start")
    public void start(@RequestParam(name = "next", required = false) String next,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {
        if (!weChatLogin.configured()) {
            redirectLogin(response, "unavailable");
            return;
        }
        try {
            rateLimiter.check("wechat:start:ip:" + RequestUtils.clientIp(request),
                    20, Duration.ofMinutes(15));
        } catch (AccountException e) {
            redirectLogin(response, "rate");
            return;
        }

        String state = randomState();
        String returnTo = safeReturnTo(next);
        writeFlowCookie(response, state, returnTo, FLOW_TTL_SECONDS);
        response.sendRedirect(weChatLogin.authorizationUrl(state));
    }

    /** 微信扫码确认后的 OAuth2 回调。 */
    @GetMapping("/callback")
    public void callback(@RequestParam(name = "code", required = false) String code,
                         @RequestParam(name = "state", required = false) String state,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        Flow flow = readFlow(request);
        clearFlowCookie(response);
        if (flow == null || state == null || !same(flow.state(), state)) {
            redirectLogin(response, "state");
            return;
        }
        if (code == null || code.isBlank()) {
            redirectLogin(response, "cancelled");
            return;
        }

        try {
            rateLimiter.check("wechat:callback:ip:" + RequestUtils.clientIp(request),
                    30, Duration.ofMinutes(15));
            weChatLogin.loginByCode(code, request, response);
            response.sendRedirect(flow.returnTo());
        } catch (AccountException e) {
            if ("ACCOUNT_DISABLED".equals(e.getCode())) {
                redirectLogin(response, "disabled");
            } else if ("RATE_LIMITED".equals(e.getCode())) {
                redirectLogin(response, "rate");
            } else if ("WECHAT_NOT_CONFIGURED".equals(e.getCode())) {
                redirectLogin(response, "unavailable");
            } else {
                redirectLogin(response, "failed");
            }
        }
    }

    private void writeFlowCookie(HttpServletResponse response, String state, String returnTo, int maxAge) {
        String encodedReturn = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(returnTo.getBytes(StandardCharsets.UTF_8));
        writeCookie(response, state + "." + encodedReturn, maxAge);
    }

    private Flow readFlow(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (!FLOW_COOKIE.equals(cookie.getName())) {
                continue;
            }
            String value = cookie.getValue();
            int split = value == null ? -1 : value.indexOf('.');
            if (split < 20 || split == value.length() - 1) {
                return null;
            }
            try {
                String state = value.substring(0, split);
                String decoded = new String(Base64.getUrlDecoder().decode(value.substring(split + 1)),
                        StandardCharsets.UTF_8);
                return new Flow(state, safeReturnTo(decoded));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private void clearFlowCookie(HttpServletResponse response) {
        writeCookie(response, "", 0);
    }

    private void writeCookie(HttpServletResponse response, String value, int maxAge) {
        StringBuilder header = new StringBuilder()
                .append(FLOW_COOKIE).append('=').append(value)
                .append("; Path=/api/account/wechat")
                .append("; Max-Age=").append(maxAge)
                .append("; HttpOnly; SameSite=Lax");
        if (secureCookie) {
            header.append("; Secure");
        }
        response.addHeader("Set-Cookie", header.toString());
    }

    /** 只允许本站路径，拒绝 scheme/authority、反斜杠和登录回环。 */
    static String safeReturnTo(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 1000) {
            return "/account";
        }
        String value = raw.trim();
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        if (!value.startsWith("/") || value.startsWith("//") || value.indexOf('\\') >= 0
                || lower.contains("%5c") || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            return "/account";
        }
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute() || uri.getRawAuthority() != null) {
                return "/account";
            }
            String path = uri.getPath();
            if (path == null || !path.startsWith("/")
                    || path.startsWith("/account/login")
                    || path.startsWith("/api/account/wechat")) {
                return "/account";
            }
            return value;
        } catch (IllegalArgumentException e) {
            return "/account";
        }
    }

    private static String randomState() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean same(String expected, String actual) {
        if (expected == null || actual == null || expected.length() != actual.length()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private static void redirectLogin(HttpServletResponse response, String status) throws IOException {
        response.sendRedirect("/account/login?wechat=" + status);
    }

    private record Flow(String state, String returnTo) {
    }
}
