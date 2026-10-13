package com.xinghuo.payment;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountIdentity;
import com.xinghuo.account.repo.AccountIdentityRepository;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.service.AccountException;
import com.xinghuo.account.service.WeChatMiniBridgeService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Re-authenticates wx.login and resolves the one mini-program openid allowed to pay for an account. */
@Service
@ConditionalOnProperty(prefix = "app.wechat-pay", name = "enabled", havingValue = "true")
public class WeChatMiniPaymentIdentityService {
    private final WeChatMiniBridgeService bridge;
    private final AccountRepository accounts;
    private final AccountIdentityRepository identities;

    public WeChatMiniPaymentIdentityService(WeChatMiniBridgeService bridge, AccountRepository accounts,
                                            AccountIdentityRepository identities) {
        this.bridge = bridge;
        this.accounts = accounts;
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public MiniPayer authenticate(String loginCode) {
        WeChatMiniBridgeService.MiniProfile profile = bridge.profile(loginCode);
        if (!profile.linked() || profile.userId() == null || profile.userId().isBlank()) {
            throw AccountException.badRequest("MINI_NOT_LINKED", "请先完成网站账号与当前微信的关联");
        }
        Account account = accounts.findByPublicUserId(profile.userId())
                .orElseThrow(() -> AccountException.serverError(
                        "WECHAT_IDENTITY_ORPHANED", "微信身份绑定异常，请联系客服"));
        if (!account.isLoginable()) {
            throw AccountException.forbidden("ACCOUNT_DISABLED", "账号已停用，请联系客服");
        }
        List<AccountIdentity> miniIdentities = identities.findByUserId(account.getId()).stream()
                .filter(identity -> identity.getProvider() == AccountIdentity.Provider.WECHAT_MINI)
                .toList();
        if (miniIdentities.isEmpty()) {
            throw AccountException.badRequest("MINI_NOT_LINKED", "请先在网站完成小程序登录确认，再使用微信支付");
        }
        if (miniIdentities.size() != 1) {
            throw AccountException.conflict("MINI_PAYMENT_IDENTITY_CONFLICT",
                    "该账号关联了多个小程序微信身份，暂不能自动支付，请联系客服核对");
        }
        String openId = miniIdentities.getFirst().getOpenId();
        if (openId == null || openId.isBlank()) {
            throw AccountException.serverError("WECHAT_IDENTITY_ORPHANED", "微信身份绑定异常，请联系客服");
        }
        return new MiniPayer(account.getId(), openId, account.getPublicUserId());
    }

    public record MiniPayer(long accountId, String openId, String userId) {}
}
