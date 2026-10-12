package com.xinghuo.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountIdentity;
import com.xinghuo.account.model.WeChatQrLogin;
import com.xinghuo.account.repo.AccountIdentityRepository;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.repo.WeChatQrLoginRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/** 微信小程序扫码确认网站登录。AppSecret 只在服务器端使用。 */
@Service
public class WeChatMiniLoginService {

    private static final String CODE2SESSION_ENDPOINT = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_ENDPOINT = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String QR_CODE_ENDPOINT = "https://api.weixin.qq.com/wxa/getwxacodeunlimit";
    private static final Duration FLOW_TTL = Duration.ofMinutes(5);
    private static final Duration ACCESS_TOKEN_SAFETY_MARGIN = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final AccountIdentityRepository identities;
    private final WeChatQrLoginRepository flows;
    private final PasswordEncoder encoder;
    private final AccountSessionService sessionService;
    private final AccountAuditService audit;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String appSecret;
    private final String envVersion;

    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpiresAt = Instant.EPOCH;

    public WeChatMiniLoginService(AccountRepository accounts,
                                  AccountIdentityRepository identities,
                                  WeChatQrLoginRepository flows,
                                  PasswordEncoder encoder,
                                  AccountSessionService sessionService,
                                  AccountAuditService audit,
                                  RestClient.Builder restClientBuilder,
                                  ObjectMapper objectMapper,
                                  @Value("${app.wechat-mini.app-id:}") String appId,
                                  @Value("${app.wechat-mini.app-secret:}") String appSecret,
                                  @Value("${app.wechat-mini.env-version:trial}") String envVersion) {
        this.accounts = accounts;
        this.identities = identities;
        this.flows = flows;
        this.encoder = encoder;
        this.sessionService = sessionService;
        this.audit = audit;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.appId = trim(appId);
        this.appSecret = trim(appSecret);
        this.envVersion = normalizeEnvVersion(envVersion);
    }

    public boolean configured() {
        return !appId.isBlank() && !appSecret.isBlank();
    }

    public String envVersion() {
        return envVersion;
    }

    /** 为网站浏览器创建一次性扫码会话。secret 只写 HttpOnly Cookie，不进入二维码。 */
    @Transactional
    public StartedFlow start(String returnPath) {
        requireConfigured();
        Instant now = Instant.now();
        String id = randomToken(18);       // Base64URL 24 字符，满足 scene <= 32 字符限制。
        String secret = randomToken(24);   // 仅浏览器持有。
        WeChatQrLogin flow = WeChatQrLogin.builder()
                .id(id)
                .browserSecretHash(sha256(secret))
                .status(WeChatQrLogin.Status.PENDING)
                .returnPath(returnPath)
                .createdAt(now)
                .expiresAt(now.plus(FLOW_TTL))
                .build();
        flows.save(flow);
        return new StartedFlow(id, secret, flow.getExpiresAt());
    }

    /** 生成带 scene 的小程序码。只有发起该会话的浏览器才能取码。 */
    @Transactional(readOnly = true)
    public byte[] qrCode(String flowId, String browserCookie) {
        WeChatQrLogin flow = requireBrowserFlow(flowId, browserCookie);
        if (flow.isExpired()) {
            throw AccountException.badRequest("WECHAT_MINI_FLOW_EXPIRED", "二维码已失效，请重新生成");
        }
        String token = accessToken();
        byte[] body = requestQrCode(token, flow.getId());
        if (looksLikeJson(body)) {
            JsonNode error = readJson(body);
            int errCode = error.path("errcode").asInt(0);
            if (errCode == 40001 || errCode == 40014 || errCode == 42001) {
                invalidateAccessToken();
                body = requestQrCode(accessToken(), flow.getId());
            }
        }
        if (looksLikeJson(body)) {
            JsonNode error = readJson(body);
            throw AccountException.serverError("WECHAT_MINI_QR_FAILED",
                    "小程序码生成失败，请稍后重试（" + error.path("errcode").asInt(-1) + "）");
        }
        return body;
    }

    /** 小程序使用 wx.login() code 确认某次网站登录。 */
    @Transactional
    public ConfirmResult confirm(String scene, String code, HttpServletRequest request) {
        requireConfigured();
        if (!looksLikeScene(scene)) {
            throw AccountException.badRequest("WECHAT_MINI_SCENE_INVALID", "登录二维码无效，请重新生成");
        }
        if (code == null || code.isBlank() || code.length() > 256) {
            throw AccountException.badRequest("WECHAT_MINI_CODE_INVALID", "微信登录凭证无效，请重新扫码");
        }

        WeChatQrLogin flow = flows.findById(scene)
                .orElseThrow(() -> AccountException.notFound("WECHAT_MINI_FLOW_NOT_FOUND", "登录二维码不存在或已失效"));
        if (flow.isExpired()) {
            flow.setStatus(WeChatQrLogin.Status.EXPIRED);
            flows.save(flow);
            throw AccountException.badRequest("WECHAT_MINI_FLOW_EXPIRED", "二维码已失效，请回到电脑重新生成");
        }
        if (flow.getStatus() == WeChatQrLogin.Status.CONFIRMED
                || flow.getStatus() == WeChatQrLogin.Status.CONSUMED) {
            return new ConfirmResult(true, "该登录已确认，可以返回电脑。", null);
        }
        if (flow.getStatus() != WeChatQrLogin.Status.PENDING) {
            throw AccountException.badRequest("WECHAT_MINI_FLOW_INVALID", "该登录会话已失效，请重新生成");
        }

        MiniIdentity miniIdentity = exchangeCode(code.trim());
        Account account = resolveAccount(miniIdentity, request);

        Instant now = Instant.now();
        flow.setUserId(account.getId());
        flow.setStatus(WeChatQrLogin.Status.CONFIRMED);
        flow.setConfirmedAt(now);
        flows.save(flow);
        audit.record(account.getId(), "WECHAT_MINI_QR_CONFIRMED", "website_login", request);
        return new ConfirmResult(true, "确认成功，电脑端将自动完成登录。", account.getDisplayName());
    }

    /** 浏览器轮询；确认后在浏览器响应中签发原有 XH_SID Cookie。 */
    @Transactional
    public StatusResult status(String flowId,
                               String browserCookie,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        WeChatQrLogin flow = requireBrowserFlow(flowId, browserCookie);
        if (flow.isExpired() && flow.getStatus() == WeChatQrLogin.Status.PENDING) {
            flow.setStatus(WeChatQrLogin.Status.EXPIRED);
            flows.save(flow);
        }

        if (flow.getStatus() == WeChatQrLogin.Status.CONFIRMED) {
            Account account = accounts.findById(flow.getUserId())
                    .orElseThrow(() -> AccountException.serverError(
                            "WECHAT_MINI_ACCOUNT_MISSING", "账号状态异常，请联系客服"));
            if (!account.isLoginable()) {
                throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已被停用，请联系客服");
            }
            flow.setStatus(WeChatQrLogin.Status.CONSUMED);
            flow.setConsumedAt(Instant.now());
            // 先把一次性会话状态落库，再签发浏览器登录 Cookie；避免并发轮询重复消费。
            flows.saveAndFlush(flow);
            sessionService.issue(account, request, response, false);
            audit.record(account.getId(), "WECHAT_MINI_LOGIN_OK", null, request);
            return new StatusResult("CONFIRMED", flow.getReturnPath(), "登录成功");
        }

        if (flow.getStatus() == WeChatQrLogin.Status.EXPIRED) {
            return new StatusResult("EXPIRED", null, "二维码已失效，请重新生成");
        }
        if (flow.getStatus() == WeChatQrLogin.Status.CONSUMED) {
            return new StatusResult("CONSUMED", flow.getReturnPath(), "登录已完成");
        }
        return new StatusResult("PENDING", null, "等待微信确认");
    }

    private Account resolveAccount(MiniIdentity mini, HttpServletRequest request) {
        Instant now = Instant.now();
        AccountIdentity identity = identities
                .findByProviderAndOpenId(AccountIdentity.Provider.WECHAT_MINI, mini.openId())
                .orElse(null);

        Account account = null;
        boolean created = false;
        if (identity != null) {
            account = accounts.findById(identity.getUserId()).orElse(null);
        }

        // 同一微信开放平台主体下，优先用 unionid 把网站微信身份与小程序身份归到同一星火账号。
        if (account == null && mini.unionId() != null) {
            AccountIdentity unionIdentity = identities
                    .findFirstByProviderAndUnionId(AccountIdentity.Provider.WECHAT, mini.unionId())
                    .orElse(null);
            if (unionIdentity != null) {
                account = accounts.findById(unionIdentity.getUserId()).orElse(null);
            }
        }

        // 兼容已经用小程序 openid 作为 WECHAT 联系凭据创建过的账号。
        if (account == null) {
            account = accounts.findByContactTypeAndContactNorm(Account.ContactType.WECHAT, mini.openId())
                    .orElse(null);
        }

        if (account == null) {
            account = createAccount(mini.openId());
            created = true;
        }
        if (!account.isLoginable()) {
            audit.record(account.getId(), "WECHAT_MINI_LOGIN_FAIL", "reason=account_status", request);
            throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已被停用，请联系客服");
        }

        if (identity == null) {
            identity = AccountIdentity.builder()
                    .userId(account.getId())
                    .provider(AccountIdentity.Provider.WECHAT_MINI)
                    .openId(mini.openId())
                    .unionId(mini.unionId())
                    .lastLoginAt(now)
                    .build();
        } else {
            identity.setUnionId(mini.unionId());
            identity.setLastLoginAt(now);
        }
        identities.save(identity);

        account.setLastLoginAt(now);
        account.setLastLoginIp(RequestUtils.clientIp(request));
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        account = accounts.save(account);
        audit.record(account.getId(), created ? "WECHAT_MINI_REGISTER" : "WECHAT_MINI_IDENTITY_OK", null, request);
        return account;
    }

    private Account createAccount(String openId) {
        Account account = Account.builder()
                .contactType(Account.ContactType.WECHAT)
                .contactValue(openId)
                .contactNorm(openId)
                .contactVerified(true)
                .publicUserId(generatePublicUserId())
                .passwordHash(encoder.encode(randomToken(32)))
                .displayName("微信用户")
                .role("BUYER")
                .levelCode("NEW")
                .status(Account.AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();
        return accounts.save(account);
    }

    private MiniIdentity exchangeCode(String code) {
        JsonNode body = getJson(CODE2SESSION_ENDPOINT
                + "?appid=" + enc(appId)
                + "&secret=" + enc(appSecret)
                + "&js_code=" + enc(code)
                + "&grant_type=authorization_code");
        throwIfWeChatError(body, "WECHAT_MINI_CODE_EXCHANGE_FAILED");
        String openId = text(body, "openid");
        if (openId.isBlank()) {
            throw AccountException.serverError("WECHAT_MINI_OPENID_MISSING", "微信登录响应缺少 openid，请重试");
        }
        String unionId = text(body, "unionid");
        return new MiniIdentity(openId, unionId.isBlank() ? null : unionId);
    }

    private byte[] requestQrCode(String token, String scene) {
        try {
            Map<String, Object> payload = Map.of(
                    "scene", scene,
                    "page", "pages/scan-login/scan-login",
                    "check_path", false,
                    "env_version", envVersion,
                    "width", 430);
            ResponseEntity<byte[]> entity = restClient.post()
                    .uri(QR_CODE_ENDPOINT + "?access_token=" + enc(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] body = entity.getBody();
            if (body == null || body.length == 0) {
                throw AccountException.serverError("WECHAT_MINI_QR_EMPTY", "小程序码生成失败，请稍后重试");
            }
            return body;
        } catch (AccountException e) {
            throw e;
        } catch (RuntimeException e) {
            throw AccountException.serverError("WECHAT_MINI_SERVICE_UNAVAILABLE", "微信服务暂时不可用，请稍后重试");
        }
    }

    private synchronized String accessToken() {
        requireConfigured();
        Instant now = Instant.now();
        if (cachedAccessToken != null
                && cachedAccessTokenExpiresAt.isAfter(now.plus(ACCESS_TOKEN_SAFETY_MARGIN))) {
            return cachedAccessToken;
        }
        JsonNode body = getJson(ACCESS_TOKEN_ENDPOINT
                + "?grant_type=client_credential"
                + "&appid=" + enc(appId)
                + "&secret=" + enc(appSecret));
        throwIfWeChatError(body, "WECHAT_MINI_ACCESS_TOKEN_FAILED");
        String token = text(body, "access_token");
        int expiresIn = body.path("expires_in").asInt(7200);
        if (token.isBlank()) {
            throw AccountException.serverError("WECHAT_MINI_ACCESS_TOKEN_MISSING", "微信服务返回异常，请稍后重试");
        }
        cachedAccessToken = token;
        cachedAccessTokenExpiresAt = now.plusSeconds(Math.max(600, expiresIn));
        return token;
    }

    private synchronized void invalidateAccessToken() {
        cachedAccessToken = null;
        cachedAccessTokenExpiresAt = Instant.EPOCH;
    }

    private JsonNode getJson(String url) {
        try {
            JsonNode body = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (body == null || body.isNull()) {
                throw AccountException.serverError("WECHAT_MINI_EMPTY_RESPONSE", "微信服务返回为空，请重试");
            }
            return body;
        } catch (AccountException e) {
            throw e;
        } catch (RuntimeException e) {
            throw AccountException.serverError("WECHAT_MINI_SERVICE_UNAVAILABLE", "微信服务暂时不可用，请稍后重试");
        }
    }

    private WeChatQrLogin requireBrowserFlow(String flowId, String browserCookie) {
        if (!looksLikeScene(flowId)) {
            throw AccountException.badRequest("WECHAT_MINI_FLOW_INVALID", "扫码登录会话无效");
        }
        CookieParts parts = CookieParts.parse(browserCookie);
        if (parts == null || !flowId.equals(parts.flowId())) {
            throw AccountException.unauthorized("WECHAT_MINI_BROWSER_MISMATCH", "该扫码登录不属于当前浏览器");
        }
        WeChatQrLogin flow = flows.findById(flowId)
                .orElseThrow(() -> AccountException.notFound("WECHAT_MINI_FLOW_NOT_FOUND", "扫码登录会话不存在"));
        if (!constantTimeEquals(flow.getBrowserSecretHash(), sha256(parts.secret()))) {
            throw AccountException.unauthorized("WECHAT_MINI_BROWSER_MISMATCH", "该扫码登录不属于当前浏览器");
        }
        return flow;
    }

    static boolean looksLikeScene(String value) {
        return value != null && value.length() >= 20 && value.length() <= 32
                && value.chars().allMatch(c -> c == '-' || c == '_'
                        || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
    }

    private static boolean looksLikeJson(byte[] body) {
        if (body == null) return false;
        for (byte b : body) {
            char c = (char) b;
            if (!Character.isWhitespace(c)) {
                return c == '{' || c == '[';
            }
        }
        return false;
    }

    private JsonNode readJson(byte[] body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw AccountException.serverError("WECHAT_MINI_RESPONSE_INVALID", "微信服务响应无法解析，请重试");
        }
    }

    private static void throwIfWeChatError(JsonNode body, String code) {
        if (body.has("errcode") && body.path("errcode").asInt(0) != 0) {
            throw AccountException.serverError(code, "微信登录未完成，请重新扫码");
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw AccountException.serverError("WECHAT_MINI_NOT_CONFIGURED", "微信小程序登录尚未完成服务器配置");
        }
    }

    private String generatePublicUserId() {
        for (int i = 0; i < 8; i++) {
            String candidate = "XH"
                    + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT)
                    + Integer.toString(RANDOM.nextInt(36 * 36 * 36), 36).toUpperCase(Locale.ROOT);
            if (!accounts.existsByPublicUserId(candidate)) {
                return candidate;
            }
        }
        throw AccountException.serverError("USER_ID_GENERATE_FAILED", "用户 ID 生成失败，请稍后重试");
    }

    private static String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : trim(value.asText(""));
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeEnvVersion(String raw) {
        String value = trim(raw).toLowerCase(Locale.ROOT);
        return switch (value) {
            case "develop", "trial", "release" -> value;
            default -> "trial";
        };
    }

    public record StartedFlow(String id, String browserSecret, Instant expiresAt) {
    }

    public record ConfirmResult(boolean ok, String message, String displayName) {
    }

    public record StatusResult(String status, String redirect, String message) {
    }

    private record MiniIdentity(String openId, String unionId) {
    }

    private record CookieParts(String flowId, String secret) {
        static CookieParts parse(String raw) {
            if (raw == null || raw.isBlank() || raw.length() > 200) return null;
            int dot = raw.indexOf('.');
            if (dot < 1 || dot == raw.length() - 1) return null;
            String flow = raw.substring(0, dot);
            String secret = raw.substring(dot + 1);
            if (!looksLikeScene(flow) || secret.length() < 20 || secret.length() > 64) return null;
            return new CookieParts(flow, secret);
        }
    }
}
