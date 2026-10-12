package com.xinghuo.account.web;

import com.xinghuo.account.service.RateLimiter;
import com.xinghuo.account.service.RequestUtils;
import com.xinghuo.account.service.WeChatMiniLoginService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 微信小程序码扫码确认网站登录。 */
@RestController
@RequestMapping("/api/account/wechat-mini")
public class AccountWeChatMiniController {

    private static final String FLOW_COOKIE = "XH_WX_MINI";
    private static final int FLOW_TTL_SECONDS = 5 * 60;

    private final WeChatMiniLoginService weChatMini;
    private final RateLimiter rateLimiter;
    private final boolean secureCookie;

    public AccountWeChatMiniController(WeChatMiniLoginService weChatMini,
                                       RateLimiter rateLimiter,
                                       @Value("${app.base-url:}") String baseUrl) {
        this.weChatMini = weChatMini;
        this.rateLimiter = rateLimiter;
        this.secureCookie = baseUrl != null && baseUrl.trim().startsWith("https://");
    }

    @GetMapping("/config")
    public ConfigResponse config() {
        return new ConfigResponse(weChatMini.configured(), weChatMini.envVersion());
    }

    @PostMapping("/start")
    public StartResponse start(@RequestBody(required = false) StartRequest body,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        rateLimiter.check("wechat-mini:start:ip:" + RequestUtils.clientIp(request),
                20, Duration.ofMinutes(15));
        String returnTo = AccountWeChatController.safeReturnTo(body == null ? null : body.next());
        WeChatMiniLoginService.StartedFlow flow = weChatMini.start(returnTo);
        writeFlowCookie(response, flow.id() + "." + flow.browserSecret(), FLOW_TTL_SECONDS);
        return new StartResponse(
                flow.id(),
                "/api/account/wechat-mini/code?flow=" + flow.id(),
                flow.expiresAt());
    }

    @GetMapping("/code")
    public ResponseEntity<byte[]> code(@RequestParam("flow") String flow,
                                       HttpServletRequest request) {
        byte[] image = weChatMini.qrCode(flow, readFlowCookie(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }

    @GetMapping("/status")
    public WeChatMiniLoginService.StatusResult status(@RequestParam("flow") String flow,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        WeChatMiniLoginService.StatusResult result = weChatMini.status(
                flow, readFlowCookie(request), request, response);
        if ("CONFIRMED".equals(result.status()) || "CONSUMED".equals(result.status())) {
            clearFlowCookie(response);
        }
        return result;
    }

    @PostMapping("/confirm")
    public WeChatMiniLoginService.ConfirmResult confirm(@Valid @RequestBody ConfirmRequest body,
                                                       HttpServletRequest request) {
        rateLimiter.check("wechat-mini:confirm:ip:" + RequestUtils.clientIp(request),
                30, Duration.ofMinutes(15));
        return weChatMini.confirm(body.scene(), body.code(), request);
    }

    private String readFlowCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (FLOW_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearFlowCookie(HttpServletResponse response) {
        writeFlowCookie(response, "", 0);
    }

    private void writeFlowCookie(HttpServletResponse response, String value, int maxAge) {
        StringBuilder header = new StringBuilder()
                .append(FLOW_COOKIE).append('=').append(value)
                .append("; Path=/api/account/wechat-mini")
                .append("; Max-Age=").append(maxAge)
                .append("; HttpOnly; SameSite=Lax");
        if (secureCookie) {
            header.append("; Secure");
        }
        response.addHeader("Set-Cookie", header.toString());
    }

    public record StartRequest(@Size(max = 1000) String next) {
    }

    public record StartResponse(String flow, String codeUrl, Instant expiresAt) {
    }

    public record ConfirmRequest(
            @NotBlank @Size(max = 32) String scene,
            @NotBlank @Size(max = 256) String code) {
    }

    public record ConfigResponse(boolean configured, String envVersion) {
    }
}
