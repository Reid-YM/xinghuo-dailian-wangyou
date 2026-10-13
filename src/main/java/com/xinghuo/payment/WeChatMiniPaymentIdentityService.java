package com.xinghuo.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountIdentity;
import com.xinghuo.account.repo.AccountIdentityRepository;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.service.AccountException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/** Re-authenticates wx.login and resolves the exact mini-program openid allowed to pay. */
@Service
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "enabled", havingValue = "true")
public class WeChatMiniPaymentIdentityService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final AccountRepository accounts;
    private final AccountIdentityRepository identities;
    private final RestClient restClient;
    private final String miniAppId;
    private final String miniAppSecret;

    public WeChatMiniPaymentIdentityService(AccountRepository accounts,
                                            AccountIdentityRepository identities,
                                            RestClient.Builder restClientBuilder,
                                            @Value("${app.wechat-mini.app-id:}") String miniAppId,
                                            @Value("${app.wechat-mini.app-secret:}") String miniAppSecret) {
        this.accounts = accounts;
        this.identities = identities;
        this.restClient = restClientBuilder.build();
        this.miniAppId = miniAppId == null ? "" : miniAppId.trim();
        this.miniAppSecret = miniAppSecret == null ? "" : miniAppSecret.trim();
    }

    @Transactional(readOnly = true)
    public MiniPayer authenticate(String loginCode) {
        String openId = exchangeOpenId(loginCode);
        AccountIdentity direct = identities.findByProviderAndOpenId(
                AccountIdentity.Provider.WECHAT_MINI, openId).orElse(null);
        if (direct == null) {
            throw AccountException.badRequest("MINI_NOT_LINKED",
                    "请先在网站完成当前微信与网站账号的关联，再使用微信支付");
        }
        Account account = accounts.findById(direct.getUserId())
                .orElseThrow(() -> AccountException.serverError(
                        "WECHAT_IDENTITY_ORPHANED", "微信身份绑定异常，请联系客服"));
        if (!account.isLoginable()) {
            throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已停用，请联系客服");
        }
        List<AccountIdentity> miniIdentities = identities.findByUserId(account.getId()).stream()
                .filter(identity -> identity.getProvider() == AccountIdentity.Provider.WECHAT_MINI)
                .toList();
        if (miniIdentities.size() != 1) {
            throw AccountException.conflict("MINI_PAYMENT_IDENTITY_CONFLICT",
                    "该账号关联了多个小程序微信身份，暂不能自动支付，请联系客服核对");
        }
        return new MiniPayer(account.getId(), openId, account.getPublicUserId(), miniAppId);
    }

    private String exchangeOpenId(String code) {
        if (miniAppId.isBlank() || miniAppSecret.isBlank()) {
            throw AccountException.serverError("WECHAT_NOT_CONFIGURED", "小程序身份服务尚未完成配置");
        }
        if (code == null || code.isBlank() || code.length() > 256) {
            throw AccountException.badRequest("WECHAT_CODE_INVALID", "微信登录凭证无效，请重试");
        }
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + enc(miniAppId)
                + "&secret=" + enc(miniAppSecret) + "&js_code=" + enc(code)
                + "&grant_type=authorization_code";
        try {
            String payload = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode body = payload == null || payload.isBlank() ? null : JSON.readTree(payload);
            if (body == null || body.path("errcode").asInt(0) != 0
                    || body.path("openid").asText("").isBlank()) {
                throw AccountException.badRequest("WECHAT_CODE_INVALID", "微信登录凭证无效，请重试");
            }
            return body.path("openid").asText();
        } catch (AccountException error) {
            throw error;
        } catch (Exception error) {
            throw AccountException.serverError("WECHAT_SERVICE_UNAVAILABLE", "微信服务暂不可用，请稍后重试");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record MiniPayer(long accountId, String openId, String userId, String miniAppId) {}
}
