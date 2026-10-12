package com.xinghuo.account.repo;

import com.xinghuo.account.model.WeChatMiniPairing;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeChatMiniPairingRepository extends JpaRepository<WeChatMiniPairing, String> {
    Optional<WeChatMiniPairing> findByCodeHash(String codeHash);
    Optional<WeChatMiniPairing> findByBrowserHash(String browserHash);

    @Modifying
    @Query("update WeChatMiniPairing p set p.consumedAt = :now "
            + "where p.id = :id and p.miniOpenId is not null "
            + "and p.consumedAt is null and p.expiresAt > :now")
    int consume(@Param("id") String id, @Param("now") Instant now);
}
