package com.xinghuo.account.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountWeChatControllerTest {

    @Test
    void safeReturnToKeepsInternalPaths() {
        assertThat(AccountWeChatController.safeReturnTo("/account#orders"))
                .isEqualTo("/account#orders");
        assertThat(AccountWeChatController.safeReturnTo("/records?q=XH123"))
                .isEqualTo("/records?q=XH123");
    }

    @Test
    void safeReturnToRejectsExternalAndAmbiguousPaths() {
        assertThat(AccountWeChatController.safeReturnTo(null)).isEqualTo("/account");
        assertThat(AccountWeChatController.safeReturnTo("https://evil.example/path")).isEqualTo("/account");
        assertThat(AccountWeChatController.safeReturnTo("//evil.example/path")).isEqualTo("/account");
        assertThat(AccountWeChatController.safeReturnTo("/\\evil.example/path")).isEqualTo("/account");
        assertThat(AccountWeChatController.safeReturnTo("/%5Cevil.example/path")).isEqualTo("/account");
    }

    @Test
    void safeReturnToPreventsLoginAndOauthLoops() {
        assertThat(AccountWeChatController.safeReturnTo("/account/login?next=/account"))
                .isEqualTo("/account");
        assertThat(AccountWeChatController.safeReturnTo("/api/account/wechat/start"))
                .isEqualTo("/account");
    }
}
