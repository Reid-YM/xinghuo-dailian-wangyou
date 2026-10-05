package com.xinghuo.account.repo;

import com.xinghuo.account.model.AccountSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountSessionRepository extends JpaRepository<AccountSession, String> {

    Optional<AccountSession> findByIdAndRevokedAtIsNull(String id);

    List<AccountSession> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtDesc(
            Long userId, Instant now);

    /**
     * 刷新最后活跃时间。
     *
     * <p>刻意用「仅当距上次刷新超过阈值才写」的写法：会话解析发生在每个请求上，
     * 无条件写库会把每次页面访问都变成一次 UPDATE。阈值由调用方传入。
     */
    @Modifying
    @Query("update AccountSession s set s.lastSeenAt = :now "
            + "where s.id = :id and s.lastSeenAt < :threshold")
    int touch(@Param("id") String id, @Param("now") Instant now, @Param("threshold") Instant threshold);

    @Modifying
    @Query("update AccountSession s set s.revokedAt = :now "
            + "where s.userId = :userId and s.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("update AccountSession s set s.revokedAt = :now "
            + "where s.userId = :userId and s.id <> :keepId and s.revokedAt is null")
    int revokeOthers(@Param("userId") Long userId, @Param("keepId") String keepId, @Param("now") Instant now);

    /** 清理任务：删除早已过期的会话行。 */
    @Modifying
    @Query("delete from AccountSession s where s.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
