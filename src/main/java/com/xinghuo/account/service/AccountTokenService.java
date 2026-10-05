package com.xinghuo.account.service;

import com.xinghuo.account.model.AccountToken;
import com.xinghuo.account.repo.AccountTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 一次性令牌：邮箱验证、找回密码。
 *
 * <p>明文令牌只出现在发给用户的邮件链接里，数据库只存 SHA-256 哈希。
 * 这样即使数据库泄露，攻击者也无法用其中的记录直接重置他人密码 ——
 * 存明文的话，一次只读泄露就等于把所有账号交出去。
 */
@Service
public class AccountTokenService {

    /** 令牌明文长度：32 字节随机，Base64URL 编码后 43 字符。 */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_BOUND = 1_000_000;

    private final AccountTokenRepository tokens;

    public AccountTokenService(AccountTokenRepository tokens) {
        this.tokens = tokens;
    }

    /**
     * 签发新令牌，返回明文（仅此一次）。
     *
     * <p>签发前先作废该用户该用途下未使用的旧令牌：否则用户连点两次
     * 「发送重置邮件」，两封邮件里的链接都会有效，泄露面无谓地翻倍。
     */
    @Transactional
    public String issue(Long userId, AccountToken.Purpose purpose, String payload,
                        Duration ttl, HttpServletRequest request) {
        tokens.invalidateOutstanding(userId, purpose, Instant.now());

        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AccountToken token = AccountToken.builder()
                .userId(userId)
                .purpose(purpose)
                .tokenHash(sha256(plaintext))
                .payload(payload)
                .expiresAt(Instant.now().plus(ttl))
                .requestIp(RequestUtils.clientIp(request))
                .build();
        tokens.save(token);
        return plaintext;
    }

    @Transactional
    public String issueCode(Long userId, AccountToken.Purpose purpose, String codeScope,
                            Duration ttl, HttpServletRequest request) {
        Instant now = Instant.now();
        if (userId == null) {
            tokens.invalidateOutstandingByPayload(codeScope, purpose, now);
        } else {
            tokens.invalidateOutstanding(userId, purpose, now);
        }
        String code = String.format("%06d", RANDOM.nextInt(CODE_BOUND));
        AccountToken token = AccountToken.builder()
                .userId(userId)
                .purpose(purpose)
                .tokenHash(sha256(codePlaintext(codeScope, code)))
                .payload(codeScope)
                .expiresAt(now.plus(ttl))
                .requestIp(RequestUtils.clientIp(request))
                .build();
        tokens.save(token);
        return code;
    }

    @Transactional
    public Optional<AccountToken> consumeCode(String codeScope, String code, AccountToken.Purpose purpose) {
        if (codeScope == null || code == null || !code.matches("\\d{6}")) {
            return Optional.empty();
        }
        String hash = sha256(codePlaintext(codeScope, code));
        if (tokens.consumeAtomically(hash, purpose, Instant.now()) != 1) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(hash);
    }

    private static String codePlaintext(String codeScope, String code) {
        return (codeScope == null ? "" : codeScope.trim().toLowerCase()) + ":" + code;
    }

    /**
     * 校验并消费令牌.
     *
     * <p>消费动作是「先置 used_at，再返回」：同一令牌并发提交两次时，
     * 只有一次能把 used_at 从 NULL 改掉（更新行数为 1）。
     */
    @Transactional
    public Optional<AccountToken> consume(String plaintext, AccountToken.Purpose purpose) {
        if (plaintext == null || plaintext.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256(plaintext);
        // 用带 usedAt IS NULL 条件的原子更新，而不是「查出来判断再保存」——
        // 后者在并发下两侧都能通过判断，令牌就不再是一次性的。
        if (tokens.consumeAtomically(hash, purpose, Instant.now()) != 1) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(hash);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必备算法，缺失说明运行环境已损坏。
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
