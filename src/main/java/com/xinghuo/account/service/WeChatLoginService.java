package com.xinghuo.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountIdentity;
import com.xinghuo.account.repo.AccountIdentityRepository;
import com.xinghuo.account.repo.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * 微信开放平台「网站应用微信登录」接入。
 *
 * <p>微信只承担外部身份认证，账号、会话、订单归属和权限仍全部落在星火账号体系：
 * {@link Account} 是唯一用户主体，{@link AccountIdentity} 只保存 WECHAT 身份映射。
 */
@Service
public class WeChatLoginService {

    private static final String AUTHORIZE_ENDPOINT = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String TOKEN_ENDPOINT = "https://api.weixin.qq.com/sns/oauth2/access_token";
    private static final String USERINFO_ENDPOINT = "https://api.weixin.qq.com/sns/userinfo";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accounts;
    private final AccountIdentityRepository identities;
    private final PasswordEncoder encoder;
    private final AccountSessionService sessionService;
    private final AccountAuditService audit;
    private final RestClient restClient;
    private final String appId;
    private final String appSecret;
    private final String baseUrl;

    public WeChatLoginService(AccountRepository accounts,
                              AccountIdentityRepository identities,
                              PasswordEncoder encoder,
                              AccountSessionService sessionService,
                              AccountAuditService audit,
                              RestClient.Builder restClientBuilder,
                              @Value("${app.wechat-login.app-id:}") String appId,
                              @Value("${app.wechat-login.app-secret:}") String appSecret,
                              @Value("${app.base-url:}") String baseUrl) {
        this.accounts = accounts;
        this.identities = identities;
        this.encoder = encoder;
        this.sessionService = sessionService;
        this.audit = audit;
        this.restClient = restClientBuilder.build();
        this.appId = trim(appId);
        this.appSecret = trim(appSecret);
        this.baseUrl = stripTrailingSlash(trim(baseUrl));
    }

    public boolean configured() {
        return !appId.isBlank() && !appSecret.isBlank() && !baseUrl.isBlank();
    }

    public String authorizationUrl(String state) {
        requireConfigured();
        String callback = baseUrl + "/api/account/wechat/callback";
        return AUTHORIZE_ENDPOINT
                + "?appid=" + enc(appId)
                + "&redirect_uri=" + enc(callback)
                + "&response_type=code"
                + "&scope=snsapi_login"
                + "&state=" + enc(state)
                + "#wechat_redirect";
    }

    /**
     * 用微信 code 换取身份并登录星火账号。首次登录创建星火账号并绑定 WECHAT 身份；
     * 后续登录只解析身份映射，不会创建第二套“微信用户”。
     */
    @Transactional
    public Account loginByCode(String code,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        requireConfigured();
        if (code == null || code.isBlank() || code.length() > 256) {
            throw AccountException.badRequest("WECHAT_CODE_INVALID", "微信登录凭证无效，请重新扫码");
        }

        WeChatProfile profile = fetchProfile(code.trim());
        Instant now = Instant.now();
        AccountIdentity identity = identities
                .findByProviderAndOpenId(AccountIdentity.Provider.WECHAT, profile.openId())
                .orElse(null);

        Account account;
        boolean created = false;
        if (identity != null) {
            account = accounts.findById(identity.getUserId())
                    .orElseThrow(() -> AccountException.serverError(
                            "WECHAT_IDENTITY_ORPHANED", "微信身份绑定异常，请联系客服"));
        } else {
            account = accounts
                    .findByContactTypeAndContactNorm(Account.ContactType.WECHAT, profile.openId())
                    .orElse(null);
            if (account == null) {
                account = createAccount(profile);
                created = true;
            }
            identity = AccountIdentity.builder()
                    .userId(account.getId())
                    .provider(AccountIdentity.Provider.WECHAT)
                    .openId(profile.openId())
                    .unionId(profile.unionId())
                    .nickname(profile.nickname())
                    .avatarUrl(profile.avatarUrl())
                    .lastLoginAt(now)
                    .build();
        }

        if (!account.isLoginable()) {
            audit.record(account.getId(), "WECHAT_LOGIN_FAIL", "reason=account_status", request);
            throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已被停用，请联系客服");
        }

        identity.setUnionId(profile.unionId());
        identity.setNickname(profile.nickname());
        identity.setAvatarUrl(profile.avatarUrl());
        identity.setLastLoginAt(now);
        identities.save(identity);

        account.setLastLoginAt(now);
        account.setLastLoginIp(RequestUtils.clientIp(request));
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        account = accounts.save(account);

        sessionService.issue(account, request, response, false);
        audit.record(account.getId(), created ? "WECHAT_REGISTER" : "WECHAT_LOGIN_OK", null, request);
        return account;
    }

    private Account createAccount(WeChatProfile profile) {
        Account account = Account.builder()
                .contactType(Account.ContactType.WECHAT)
                .contactValue(profile.openId())
                .contactNorm(profile.openId())
                .contactVerified(true)
                .publicUserId(generatePublicUserId())
                // 第三方账号没有可用于密码登录的初始密码；随机高熵哈希只用于满足主表约束。
                .passwordHash(encoder.encode(randomOpaqueSecret()))
                .displayName(displayName(profile.nickname()))
                .avatarUrl(profile.avatarUrl())
                .role("BUYER")
                .levelCode("NEW")
                .status(Account.AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();
        return accounts.save(account);
    }

    private WeChatProfile fetchProfile(String code) {
        JsonNode token = getJson(TOKEN_ENDPOINT
                + "?appid=" + enc(appId)
                + "&secret=" + enc(appSecret)
                + "&code=" + enc(code)
                + "&grant_type=authorization_code");
        throwIfWeChatError(token, "WECHAT_TOKEN_FAILED");

        String accessToken = text(token, "access_token");
        String openId = text(token, "openid");
        if (accessToken.isBlank() || openId.isBlank()) {
            throw AccountException.serverError("WECHAT_TOKEN_INVALID", "微信登录响应缺少必要字段，请重试");
        }

        JsonNode user = getJson(USERINFO_ENDPOINT
                + "?access_token=" + enc(accessToken)
                + "&openid=" + enc(openId)
                + "&lang=zh_CN");
        throwIfWeChatError(user, "WECHAT_USERINFO_FAILED");

        String unionId = text(user, "unionid");
        if (unionId.isBlank()) {
            unionId = text(token, "unionid");
        }
        return new WeChatProfile(
                openId,
                blankToNull(unionId),
                blankToNull(limit(text(user, "nickname"), 120)),
                normalizeAvatar(text(user, "headimgurl")));
    }

    private JsonNode getJson(String url) {
        try {
            JsonNode body = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (body == null || body.isNull()) {
                throw AccountException.serverError("WECHAT_EMPTY_RESPONSE", "微信登录服务返回为空，请重试");
            }
            return body;
        } catch (AccountException e) {
            throw e;
        } catch (RuntimeException e) {
            throw AccountException.serverError("WECHAT_SERVICE_UNAVAILABLE", "微信登录服务暂时不可用，请稍后重试");
        }
    }

    private static void throwIfWeChatError(JsonNode body, String code) {
        if (body.has("errcode") && body.path("errcode").asInt(0) != 0) {
            throw AccountException.serverError(code, "微信登录未完成，请重新扫码");
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw AccountException.serverError("WECHAT_NOT_CONFIGURED", "微信登录尚未完成配置");
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

    private static String randomOpaqueSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String displayName(String nickname) {
        String value = trim(nickname);
        if (value.isBlank()) {
            return "微信用户";
        }
        return limit(value, 60);
    }

    private static String normalizeAvatar(String avatar) {
        String value = trim(avatar);
        if (value.isBlank() || value.length() > 500 || !value.startsWith("https://")) {
            return null;
        }
        return value;
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

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String limit(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record WeChatProfile(String openId, String unionId, String nickname, String avatarUrl) {
    }
}
