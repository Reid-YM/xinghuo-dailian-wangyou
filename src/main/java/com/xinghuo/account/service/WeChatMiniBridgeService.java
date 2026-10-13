package com.xinghuo.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountIdentity;
import com.xinghuo.account.model.WeChatMiniPairing;
import com.xinghuo.account.repo.AccountIdentityRepository;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.repo.WeChatMiniPairingRepository;
import com.xinghuo.account.service.QuoteContactService;
import com.xinghuo.model.UploadAsset;
import com.xinghuo.service.UploadService;
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
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/** 网站与小程序通过微信 code2session 和短期人工确认码建立同一星火账号。 */
@Service
public class WeChatMiniBridgeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final String TOKEN_ENDPOINT = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_ENDPOINT = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private final AccountRepository accounts;
    private final AccountIdentityRepository identities;
    private final WeChatMiniPairingRepository pairings;
    private final PasswordEncoder encoder;
    private final OrderLinkService orders;
    private final UploadService uploads;
    private final QuoteContactService contacts;

    /** access_token 内存缓存（微信侧有效期 7200 秒，这里保守取 7000 秒）。 */
    private volatile String cachedToken = "";
    private volatile Instant cachedTokenUntil = Instant.EPOCH;
    /** 微信 jscode2session 返回 text/plain，需要自行解析（见 exchange）。 */
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;
    private final String appId;
    private final String appSecret;

    public WeChatMiniBridgeService(AccountRepository accounts, AccountIdentityRepository identities,
                                   WeChatMiniPairingRepository pairings, PasswordEncoder encoder,
                                   OrderLinkService orders, UploadService uploads, QuoteContactService contacts,
                                   RestClient.Builder restClientBuilder,
                                   @Value("${app.wechat-mini.app-id:}") String appId,
                                   @Value("${app.wechat-mini.app-secret:}") String appSecret) {
        this.accounts = accounts;
        this.identities = identities;
        this.pairings = pairings;
        this.encoder = encoder;
        this.orders = orders;
        this.uploads = uploads;
        this.contacts = contacts;
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

    /**
     * 手机号快速验证：小程序授权后，把手机号存到**已关联**账号的「联系方式」里
     * （账号中心会显示为「手机」，客服回访直接用；不做数据库迁移、不影响登录凭据）。
     *
     * <p>需要服务端 access_token：由本机 IP 调 cgi-bin/token 获取，
     * <b>生产服务器 IP 必须在小程序后台的「开发设置 → IP 白名单」里</b>，
     * 否则微信返回 errcode 40164，这里会把它带进错误信息便于排障。
     */
    @Transactional
    public String syncPhone(String loginCode, String phoneCode) {
        MiniIdentity mini = exchange(loginCode);
        Account account = findLinked(mini);
        if (account == null || !account.isLoginable()) {
            throw AccountException.badRequest("MINI_NOT_LINKED",
                    "请先在网站获取登录码并完成账号关联，再授权手机号");
        }
        if (phoneCode == null || phoneCode.isBlank() || phoneCode.length() > 256) {
            throw AccountException.badRequest("PHONE_CODE_INVALID", "手机号授权凭证无效，请重试");
        }
        String phone = fetchPhoneNumber(phoneCode);
        String normalized = phone.startsWith("+86") ? phone.substring(3) : phone;
        normalized = normalized.trim();
        if (!normalized.matches("[0-9]{5,20}")) {
            throw AccountException.serverError("PHONE_UNAVAILABLE", "没取到有效手机号，请稍后再试");
        }
        contacts.add(account, "PHONE", normalized);
        return maskPhone(normalized);
    }

    private String fetchPhoneNumber(String phoneCode) {
        String url = PHONE_ENDPOINT + "?access_token=" + enc(accessToken());
        try {
            String payload = restClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"code\":\"" + enc(phoneCode) + "\"}")
                    .retrieve().body(String.class);
            JsonNode body = payload == null || payload.isBlank() ? null : JSON.readTree(payload);
            if (body == null || body.path("errcode").asInt(0) != 0) {
                throw AccountException.badRequest("PHONE_CODE_INVALID", "手机号授权已失效，请重新点击授权");
            }
            return body.path("phone_info").path("phoneNumber").asText("");
        } catch (AccountException e) {
            throw e;
        } catch (Exception e) {
            throw AccountException.serverError("PHONE_SERVICE_UNAVAILABLE", "微信服务暂不可用，请稍后重试");
        }
    }

    /**
     * 取 access_token。加锁的原因：微信 access_token 重复获取会让旧的失效，
     * 并发首次调用时若两次同时发出，其中一个请求就会拿到已被顶掉的 token 而失败。
     * 这里用 synchronized 保证同一时刻只有一个线程去获取，其余线程直接命中缓存。
     */
    private synchronized String accessToken() {
        if (!cachedToken.isBlank() && Instant.now().isBefore(cachedTokenUntil)) {
            return cachedToken;
        }
        String url = TOKEN_ENDPOINT + "?grant_type=client_credential&appid=" + enc(appId)
                + "&secret=" + enc(appSecret);
        try {
            String payload = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode body = payload == null || payload.isBlank() ? null : JSON.readTree(payload);
            String token = body == null ? "" : body.path("access_token").asText("");
            if (token.isBlank()) {
                String code = body == null ? "" : body.path("errcode").asText("");
                throw AccountException.serverError("WECHAT_TOKEN_FAILED",
                        "获取微信凭据失败" + (code.isBlank() ? "" : "（errcode " + code + "）"));
            }
            cachedToken = token;
            cachedTokenUntil = Instant.now().plusSeconds(7000);
            return token;
        } catch (AccountException e) {
            throw e;
        } catch (Exception e) {
            throw AccountException.serverError("WECHAT_TOKEN_FAILED", "获取微信凭据失败，请稍后再试");
        }
    }

    private static String maskPhone(String phone) {
        if (phone.length() < 7) return "";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 小程序把在微信侧授权到的昵称 / 头像同步到**已关联**的网站账号。
     *
     * <p>身份仍由一次性微信 code 校验；头像复用网站既有的用户头像存储
     * （UploadService#storeImage + ACCOUNT_AVATAR 绑定），与网站账号设置里
     * 上传头像是同一套资源，因此两端显示同一个地址。
     * 只补充资料，不创建账号、不改变关联关系。
     */
    @Transactional
    public MiniProfile syncProfile(String loginCode, String nickName, MultipartFile avatar) {
        MiniIdentity mini = exchange(loginCode);
        Account account = findLinked(mini);
        if (account == null || !account.isLoginable()) {
            throw AccountException.badRequest("MINI_NOT_LINKED",
                    "请先在网站获取登录码并完成账号关联，再同步资料");
        }

        boolean changed = false;
        String name = nickName == null ? "" : nickName.trim();
        if (!name.isEmpty() && !name.equals(account.getDisplayName())) {
            account.setDisplayName(name.length() > 60 ? name.substring(0, 60) : name);
            changed = true;
        }
        if (avatar != null && !avatar.isEmpty()) {
            UploadAsset asset = uploads.storeImage(avatar, UploadAsset.Kind.USER_AVATAR);
            uploads.bind(asset.getId(), "ACCOUNT_AVATAR", String.valueOf(account.getId()));
            account.setAvatarUrl("/api/site-assets/" + asset.getId());
            changed = true;
        }
        Account saved = changed ? accounts.save(account) : account;

        return new MiniProfile(true,
                saved.getDisplayName() == null ? "" : saved.getDisplayName(),
                saved.getAvatarUrl() == null ? "" : saved.getAvatarUrl(),
                saved.getPublicUserId(),
                orders.countOrders(saved.getId()));
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
            // 微信 jscode2session 的响应头是 Content-Type: text/plain，
            // 而 Spring 的 Jackson 转换器只接受 application/json / application/*+json，
            // 直接 .body(JsonNode.class) 会抛 no suitable HttpMessageConverter，
            // 被下面的 catch 统一变成 502。这里先按字符串取回再自行解析。
            String payload = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode body = payload == null || payload.isBlank() ? null : JSON.readTree(payload);
            if (body == null || body.path("errcode").asInt(0) != 0
                    || body.path("openid").asText("").isBlank()) {
                throw AccountException.badRequest("WECHAT_CODE_INVALID", "微信登录凭证无效，请重试");
            }
            String openId = body.path("openid").asText();
            String unionId = body.path("unionid").asText("");
            return new MiniIdentity(openId, unionId.isBlank() ? null : unionId);
        } catch (AccountException e) {
            throw e;
        } catch (Exception e) {
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
