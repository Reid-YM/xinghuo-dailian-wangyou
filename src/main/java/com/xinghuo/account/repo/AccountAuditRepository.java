package com.xinghuo.account.repo;

import com.xinghuo.account.model.AccountAudit;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAuditRepository extends JpaRepository<AccountAudit, Long> {

    List<AccountAudit> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByActionAndIpAndCreatedAtAfter(String action, String ip, Instant since);
}
