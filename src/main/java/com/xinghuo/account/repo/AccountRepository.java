package com.xinghuo.account.repo;

import com.xinghuo.account.model.Account;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByContactTypeAndContactNorm(Account.ContactType contactType, String contactNorm);

    Optional<Account> findByEmailNorm(String emailNorm);

    Optional<Account> findByPublicUserId(String publicUserId);

    boolean existsByContactTypeAndContactNorm(Account.ContactType contactType, String contactNorm);

    boolean existsByEmailNorm(String emailNorm);

    boolean existsByPublicUserId(String publicUserId);

    /** 用于注册频率统计与风控。 */
    long countByCreatedAtAfter(Instant since);
}
