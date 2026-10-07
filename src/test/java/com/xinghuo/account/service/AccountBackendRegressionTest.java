package com.xinghuo.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinghuo.account.model.Account;
import com.xinghuo.account.model.AccountToken;
import com.xinghuo.account.repo.AccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountBackendRegressionTest {

    @Test
    void invalidQuoteContactTypeReturnsBadRequestInsteadOfNullPointer() {
        AccountRepository accounts = mock(AccountRepository.class);
        QuoteContactService service = new QuoteContactService(accounts, new ObjectMapper());
        Account account = Account.builder().id(1L).build();

        AccountException error = assertThrows(AccountException.class,
                () -> service.add(account, "EMAIL", "user@example.com"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertEquals("QUOTE_CONTACT_INVALID_TYPE", error.getCode());
        verify(accounts, never()).save(any(Account.class));
    }

    @Test
    void verifyingSecurityEmailDoesNotReplaceWechatPrimaryIdentity() {
        AccountRepository accounts = mock(AccountRepository.class);
        AccountTokenService tokens = mock(AccountTokenService.class);
        AccountSessionService sessions = mock(AccountSessionService.class);
        SmtpMailSender mail = mock(SmtpMailSender.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AccountAuditService audit = mock(AccountAuditService.class);
        RateLimiter limiter = mock(RateLimiter.class);
        AccountEmailService service = new AccountEmailService(
                accounts, tokens, sessions, mail, encoder, audit, limiter, "https://example.test");

        Account account = Account.builder()
                .id(7L)
                .contactType(Account.ContactType.WECHAT)
                .contactValue("wechat-open-id")
                .contactNorm("wechat-open-id")
                .contactVerified(true)
                .email("user@example.com")
                .emailNorm("user@example.com")
                .emailVerified(false)
                .build();
        AccountToken token = AccountToken.builder().userId(7L).build();
        when(tokens.consumeCode(anyString(), eq("123456"), eq(AccountToken.Purpose.VERIFY_EMAIL)))
                .thenReturn(Optional.of(token));
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.verifyEmailCode(account, "123456", mock(HttpServletRequest.class));

        assertTrue(account.isEmailVerified());
        assertEquals(Account.ContactType.WECHAT, account.getContactType());
        assertEquals("wechat-open-id", account.getContactValue());
        assertEquals("wechat-open-id", account.getContactNorm());
        verify(accounts).save(account);
    }

    @Test
    void verifyingEmailAccountKeepsEmailCredentialSynchronized() {
        AccountRepository accounts = mock(AccountRepository.class);
        AccountTokenService tokens = mock(AccountTokenService.class);
        AccountSessionService sessions = mock(AccountSessionService.class);
        SmtpMailSender mail = mock(SmtpMailSender.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AccountAuditService audit = mock(AccountAuditService.class);
        RateLimiter limiter = mock(RateLimiter.class);
        AccountEmailService service = new AccountEmailService(
                accounts, tokens, sessions, mail, encoder, audit, limiter, "https://example.test");

        Account account = Account.builder()
                .id(8L)
                .contactType(Account.ContactType.EMAIL)
                .contactValue("old@example.com")
                .contactNorm("old@example.com")
                .contactVerified(false)
                .email("new@example.com")
                .emailNorm("new@example.com")
                .emailVerified(false)
                .build();
        when(tokens.consumeCode(anyString(), eq("654321"), eq(AccountToken.Purpose.VERIFY_EMAIL)))
                .thenReturn(Optional.of(AccountToken.builder().userId(8L).build()));
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.verifyEmailCode(account, "654321", mock(HttpServletRequest.class));

        assertTrue(account.isEmailVerified());
        assertTrue(account.isContactVerified());
        assertEquals(Account.ContactType.EMAIL, account.getContactType());
        assertEquals("new@example.com", account.getContactValue());
        assertEquals("new@example.com", account.getContactNorm());
    }
}
