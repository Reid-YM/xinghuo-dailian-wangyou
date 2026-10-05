package com.xinghuo.account.repo;

import com.xinghuo.account.model.AccountToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

    Optional<AccountToken> findByTokenHash(String tokenHash);

    /**
     * 原子地消费令牌，返回受影响行数。
     *
     * <p>必须用它而不是「查出来 → setUsedAt → save」：后者是 check-then-act，
     * 实体没有 @Version，Hibernate 发出的 UPDATE 不带 usedAt IS NULL 条件，
     * 两个并发请求都能通过 isUsable() 判断并各自提交成功，一次性就失效了。
     * 这里把「未使用」写进 WHERE，只有一行被改动才算消费成功。
     */
    @Modifying
    @Query("update AccountToken t set t.usedAt = :now "
            + "where t.tokenHash = :hash and t.purpose = :purpose "
            + "and t.usedAt is null and t.expiresAt > :now")
    int consumeAtomically(@Param("hash") String hash,
                          @Param("purpose") AccountToken.Purpose purpose,
                          @Param("now") Instant now);

    /**
     * 作废某用户某用途下尚未使用的一次性令牌。
     *
     * <p>每次签发新令牌前调用，保证同一用途同时只有一个有效令牌，
     * 避免用户连点两次「发送重置邮件」后两封邮件里的链接都还能用。
     */
    @Modifying
    @Query("update AccountToken t set t.usedAt = :now "
            + "where t.userId = :userId and t.purpose = :purpose and t.usedAt is null")
    int invalidateOutstanding(@Param("userId") Long userId,
                              @Param("purpose") AccountToken.Purpose purpose,
                              @Param("now") Instant now);

    /**
     * 作废同一用途、同一作用域下尚未使用的一次性令牌。
     *
     * <p>注册验证码签发时账号还不存在，不能按 userId 归属，只能按 payload 中的
     * 邮箱作用域归属；否则会被 user_token.user_id 外键拒绝。
     */
    @Modifying
    @Query("update AccountToken t set t.usedAt = :now "
            + "where t.payload = :payload and t.purpose = :purpose and t.usedAt is null")
    int invalidateOutstandingByPayload(@Param("payload") String payload,
                                       @Param("purpose") AccountToken.Purpose purpose,
                                       @Param("now") Instant now);

    @Modifying
    @Query("delete from AccountToken t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
