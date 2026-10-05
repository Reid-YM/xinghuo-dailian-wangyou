package com.xinghuo.account.repo;

import com.xinghuo.account.model.ProgressEntry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgressRepository extends JpaRepository<ProgressEntry, Long> {

    Optional<ProgressEntry> findByUserIdAndToolKey(Long userId, String toolKey);

    List<ProgressEntry> findByUserIdOrderByUpdatedAtDesc(Long userId);

    long countByUserId(Long userId);

    /**
     * 带版本条件的原子更新，返回受影响行数。
     *
     * <p>替代「查出来比较版本再 save」的 check-then-act：后者在并发提交
     * 相同 version 时两侧都会通过检查，与「后提交者拿到 409」的承诺不符。
     * 把版本条件写进 WHERE，只有真正匹配的那一次能改到行。
     */
    @Modifying
    @Query("update ProgressEntry p set p.payload = :payload, "
            + "p.version = p.version + 1, p.updatedAt = :now "
            + "where p.userId = :userId and p.toolKey = :toolKey and p.version = :expected")
    int updateIfVersionMatches(@Param("userId") Long userId,
                               @Param("toolKey") String toolKey,
                               @Param("expected") int expected,
                               @Param("payload") String payload,
                               @Param("now") Instant now);
}
