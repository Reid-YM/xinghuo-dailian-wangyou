package com.xinghuo.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountIdentity;
import com.xinghuo.account.model.WeChatMiniPairing;
import com.xinghuo.account.repo.AccountIdentityRepository;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.repo.WeChatMiniPairingRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/** 网站与小程序通过微信 code2session 和短期人工确认码建立同一星火账号。 */
@Service
public class WeChatMiniBridgeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private final AccountRepository accounts;
    private final AccountIdentityRepository identities;
    private final WeChatMiniPairingRepository pairings;
    private final PasswordEncoder encoder;
    private final OrderLinkService orders;
    private final RestClient restClient;
    private final String appId;
    private final String appSecret;

    public WeChatMiniBridgeService(AccountRepository accounts, AccountIdentityRepository identities,
                                   WeChatMiniPairingRepository pairings, PasswordEncoder encoder,
                                   OrderLinkService orders,
                                   RestClient.Builder restClientBuilder,
                                   @Value("${app.wechat-mini.app-id:}") String appId,
                                   @Value("${app.wechat-mini.app-secret:}") String appSecret) {
        this.accounts = accounts;
        this.identities = identities;
        this.pairings = pairings;
        this.encoder = encoder;
        this.orders = orders;
        this.restClient = restClientBuilder.build();
        this.appId = appId == null ? "" : appId.trim();
        this.appSecret = appSecret == null ? "" : appSecret.trim();
    }

    public boolean configured() {
        return !appId.isBlank() && !appSecret.isBlank();
    }

    @Transactional
    public Challenge issue(Long targetUserId) {
        requireConfigured();
        String browserSecret = randomUrl(32);
        String code = null;
        String codeHash = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            String candidate = String.format(Locale.ROOT, "%08d", RANDOM.nextInt(100_000_000));
            String candidateHash = sha256(candidate);
            if (pairings.existsByCodeHash(candidateHash)) continue;
            code = candidate;
            codeHash = candidateHash;
            break;
        }
        if (code == null) throw AccountException.serverError(
                "PAIR_CODE_UNAVAILABLE", "暂时无法生成登录码，请稍后重试");
        WeChatMiniPairing pairing = new WeChatMiniPairing();
        pairing.setId(randomUrl(32));
        pairing.setCodeHash(codeHash);
        pairing.setBrowserHash(sha256(browserSecret));
        pairing.setTargetUserId(targetUserId);
        pairing.setCreatedAt(Instant.now());
        pairing.setExpiresAt(Instant.now().plus(CHALLENGE_TTL));
        pairings.save(pairing);
        return new Challenge(code, browserSecret, CHALLENGE_TTL.toSeconds(), targetUserId != null);
    }

    /** 浏览器必须持有 HttpOnly Cookie；仅凭可见的八位码不能领取网页登录会话。 */
    @Transactional
    public PollResult poll(String browserSecret) {
        if (browserSecret == null || browserSecret.isBlank()) {
            return new PollResult("expired", "");
        }
        WeChatMiniPairing pairing = pairings.findByBrowserHash(sha256(browserSecret)).orElse(null);
        if (pairing == null || !pairing.getExpiresAt().isAfter(Instant.now())
                || pairing.getConsumedAt() != null) {
            return new PollResult("expired", "");
        }
        String openId = pairing.getMiniOpenId();
        return new PollResult(openId == null ? "pending" : "ready",
                openId == null ? "" : openId.substring(Math.max(0, openId.length() - 6)));
    }

    /** 原浏览器二次确认后才绑定身份并签发会话，防止泄露的短码被用于账号接管。 */
    @Transactional
    public Account complete(String browserSecret, Long currentUserId) {
        if (browserSecret == null || browserSecret.isBlank()) {
            throw AccountException.badRequest("PAIR_EXPIRED", "登录码已失效，请重新获取");
        }
        WeChatMiniPairing pairing = pairings.findByBrowserHash(sha256(browserSecret)).orElse(null);
        if (pairing == null || pairing.getMiniOpenId() == null || pairing.getConsumedAt() != null
                || !pairing.getExpiresAt().isAfter(Instant.now())) {
            throw AccountException.badRequest("PAIR_EXPIRED", "登录码已失效，请重新获取");
        }
        MiniIdentity mini = new MiniIdentity(pairing.getMiniOpenId(), pairing.getMiniUnionId());
        if (pairing.getTargetUserId() != null && !pairing.getTargetUserId().equals(currentUserId)) {
            throw AccountException.unauthenticated();
        }
        Account linked = findLinked(mini);
        Account account;
        if (pairing.getTargetUserId() != null) {
            account = accounts.findById(pairing.getTargetUserId())
                    .orElseThrow(AccountException::unauthenticated);
            if (linked != null && !linked.getId().equals(account.getId())) {
                throw AccountException.conflict("PAIR_ACCOUNT_CONFLICT", "该微信已关联其他星火账号，请联系客服核对");
            }
        } else {
            account = linked == null ? createAccount(mini.openId()) : linked;
        }
        if (!account.isLoginable()) {
            throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已停用，请联系客服");
        }
        AccountIdentity identity = identities.findByProviderAndOpenId(
                AccountIdentity.Provider.WECHAT_MINI, mini.openId()).orElse(null);
        if (identity == null) {
            identity = AccountIdentity.builder().provider(AccountIdentity.Provider.WECHAT_MINI)
                    .openId(mini.openId()).userId(account.getId()).build();
        }
        identity.setUnionId(mini.unionId());
        identity.setLastLoginAt(Instant.now());
        identities.save(identity);
        pairing.setApprovedUserId(account.getId());
        pairing.setApprovedAt(Instant.now());
        if (pairings.consume(pairing.getId(), Instant.now()) != 1) {
            throw AccountException.badRequest("PAIR_EXPIRED", "登录码已失效，请重新获取");
        }
        return account;
    }

    @Transactional(readOnly = true)
    public MiniProfile profile(String loginCode) {
        MiniIdentity mini = exchange(loginCode);
        Account account = findLinked(mini);
        return account == null || !account.isLoginable() ? new MiniProfile(false, "", "", "", 0)
                : new MiniProfile(true, account.getDisplayName(),
                        account.getAvatarUrl() == null ? "" : account.getAvatarUrl(),
                        account.getPublicUserId(), orders.countOrders(account.getId()));
    }

    @Transactional
    public MiniProfile confirm(String code, String loginCode) {
        requireConfigured();
        if (code == null || !code.matches("[0-9]{8}")) {
            throw AccountException.badRequest("PAIR_CODE_INVALID", "请输入网站显示的八位登录码");
        }
        WeChatMiniPairing pairing = pairings.findByCodeHash(sha256(code)).orElse(null);
        if (pairing == null || pairing.getMiniOpenId() != null || pairing.getConsumedAt() != null
                || !pairing.getExpiresAt().isAfter(Instant.now())) {
            throw AccountException.badRequest("PAIR_CODE_INVALID", "登录码无效或已过期，请在网站重新获取");
        }
        MiniIdentity mini = exchange(loginCode);
        // 此处只记录微信校验结果。账号绑定由原浏览器二次确认完成。
        pairing.setMiniOpenId(mini.openId());
        pairing.setMiniUnionId(mini.unionId());
        pairings.save(pairing);
        Account linked = findLinked(mini);
        return linked == null || !linked.isLoginable() ? new MiniProfile(false, "", "", "", 0)
                : new MiniProfile(true, linked.getDisplayName(),
                        linked.getAvatarUrl() == null ? "" : linked.getAvatarUrl(),
                        linked.getPublicUserId(), orders.countOrders(linked.getId()));
    }

    private Account findLinked(MiniIdentity mini) {
        AccountIdentity direct = identities.findByProviderAndOpenId(
                AccountIdentity.Provider.WECHAT_MINI, mini.openId()).orElse(null);
        Long unionUserId = null;
        if (mini.unionId() != null && !mini.unionId().isBlank()) {
            List<AccountIdentity> sameUnion = identities.findByUnionIdAndProviderIn(mini.unionId(),
                    List.of(AccountIdentity.Provider.WECHAT, AccountIdentity.Provider.WECHAT_MINI));
            if (!sameUnion.isEmpty()) {
                unionUserId = sameUnion.get(0).getUserId();
                Long expected = unionUserId;
                if (sameUnion.stream().anyMatch(link -> !expected.equals(link.getUserId()))) {
                    throw AccountException.conflict("PAIR_ACCOUNT_CONFLICT", "微信身份关联到多个账号，请联系客服核对");
                }
            }
        }
        if (direct != null && unionUserId != null && !direct.getUserId().equals(unionUserId)) {
            throw AccountException.conflict("PAIR_ACCOUNT_CONFLICT", "微信身份关联到多个账号，请联系客服核对");
        }
        Long userId = direct != null ? direct.getUserId() : unionUserId;
        if (userId == null) return null;
        return accounts.findById(userId).orElseThrow(() -> AccountException.serverError(
                "WECHAT_IDENTITY_ORPHANED", "微信身份绑定异常，请联系客服"));
    }

    private Account createAccount(String openId) {
        String contact = "mini:" + openId;
        if (contact.length() > 120) {
            throw AccountException.serverError("WECHAT_ID_INVALID", "微信身份格式异常");
        }
        for (int i = 0; i < 8; i++) {
            String publicId = "XH" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT)
                    + Integer.toString(RANDOM.nextInt(36 * 36 * 36), 36).toUpperCase(Locale.ROOT);
            if (accounts.existsByPublicUserId(publicId)) continue;
            Account account = Account.builder()
                    .contactType(Account.ContactType.WECHAT).contactValue(contact).contactNorm(contact)
                    .contactVerified(true).publicUserId(publicId)
                    .passwordHash(encoder.encode(randomUrl(32))).displayName("微信用户")
                    .role("BUYER").levelCode("NEW").status(Account.AccountStatus.ACTIVE).build();
            return accounts.save(account);
        }
        throw AccountException.serverError("USER_ID_GENERATE_FAILED", "账号创建失败，请重试");
    }

    private MiniIdentity exchange(String code) {
        requireConfigured();
        if (code == null || code.isBlank() || code.length() > 256) {
            throw AccountException.badRequest("WECHAT_CODE_INVALID", "微信登录凭证无效，请重试");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + enc(appId)
                + "&secret=" + enc(appSecret) + "&js_code=" + enc(code)
                + "&grant_type=authorization_code";
        try {
            JsonNode body = restClient.get().uri(url).retrieve().body(JsonNode.class);
            if (body == null || body.path("errcode").asInt(0) != 0
                    || body.path("openid").asText("").isBlank()) {
                throw AccountException.badRequest("WECHAT_CODE_INVALID", "微信登录凭证无效，请重试");
            }
            String openId = body.path("openid").asText();
            String unionId = body.path("unionid").asText("");
            return new MiniIdentity(openId, unionId.isBlank() ? null : unionId);
        } catch (AccountException e) {
            throw e;
        } catch (RuntimeException e) {
            throw AccountException.serverError("WECHAT_SERVICE_UNAVAILABLE", "微信服务暂不可用，请稍后重试");
        }
    }

    private void requireConfigured() {
        if (!configured()) throw AccountException.serverError(
                "WECHAT_NOT_CONFIGURED", "小程序网站互通尚未完成配置");
    }

    private static String randomUrl(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record Challenge(String code, String browserSecret, long expiresIn, boolean binding) {}
    public record PollResult(String status, String identityHint) {}
    public record MiniProfile(boolean linked, String displayName, String avatarUrl,
                              String userId, long orderCount) {}
    private record MiniIdentity(String openId, String unionId) {}
}
