package com.xinghuo.account.repo;

import com.xinghuo.account.model.AccountIdentity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountIdentityRepository extends JpaRepository<AccountIdentity, Long> {

    Optional<AccountIdentity> findByProviderAndOpenId(AccountIdentity.Provider provider, String openId);

    List<AccountIdentity> findByUserId(Long userId);
}
