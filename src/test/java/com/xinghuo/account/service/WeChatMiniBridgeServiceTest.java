package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountIdentity;
import com.xinghuo.account.model.WeChatMiniPairing;
import com.xinghuo.account.repo.AccountIdentityRepository;
import com.xinghuo.account.repo.AccountRepository;
import com.xinghuo.account.repo.WeChatMiniPairingRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

class WeChatMiniBridgeServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountIdentityRepository identities = mock(AccountIdentityRepository.class);
    private final WeChatMiniPairingRepository pairings = mock(WeChatMiniPairingRepository.class);
    private final WeChatMiniBridgeService service = new WeChatMiniBridgeService(
            accounts, identities, pairings, mock(PasswordEncoder.class),
            mock(OrderLinkService.class), RestClient.builder(), "mini-app", "secret");

    @Test
    void visibleCodeAloneCannotCompleteLogin() {
        assertThrows(AccountException.class, () -> service.complete("wrong-browser-secret", null));
        verify(pairings, never()).consume(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bindingCannotMoveToAnotherWebsiteSession() {
        WeChatMiniPairing pairing = readyPairing();
        pairing.setTargetUserId(1L);
        when(pairings.findByBrowserHash(anyString())).thenReturn(Optional.of(pairing));

        assertThrows(AccountException.class, () -> service.complete("browser-secret", 2L));
        verify(pairings, never()).consume(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bindingRejectsWechatIdentityOwnedByAnotherAccount() {
        WeChatMiniPairing pairing = readyPairing();
        pairing.setTargetUserId(1L);
        when(pairings.findByBrowserHash(anyString())).thenReturn(Optional.of(pairing));
        when(identities.findByProviderAndOpenId(AccountIdentity.Provider.WECHAT_MINI, "mini-open-id"))
                .thenReturn(Optional.of(AccountIdentity.builder().userId(2L)
                        .provider(AccountIdentity.Provider.WECHAT_MINI)
                        .openId("mini-open-id").build()));
        when(accounts.findById(2L)).thenReturn(Optional.of(Account.builder().id(2L).build()));

        assertThrows(AccountException.class, () -> service.complete("browser-secret", 1L));
        verify(pairings, never()).consume(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    private static WeChatMiniPairing readyPairing() {
        WeChatMiniPairing pairing = new WeChatMiniPairing();
        pairing.setId("pairing-id");
        pairing.setMiniOpenId("mini-open-id");
        pairing.setExpiresAt(Instant.now().plusSeconds(60));
        return pairing;
    }
}
