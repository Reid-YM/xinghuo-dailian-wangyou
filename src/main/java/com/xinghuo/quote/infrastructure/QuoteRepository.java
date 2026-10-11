package com.xinghuo.quote.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface QuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {
    @Query(value="select distinct q.utmSource from Quote q where q.utmSource is not null order by q.utmSource")
    List<String> distinctUtmSources();

    Optional<Quote> findByIdempotencyKey(String idempotencyKey);

    Optional<Quote> findByBusinessNo(String businessNo);

    List<Quote> findTop100ByOrderByCreatedAtDesc();

    List<Quote> findTop50ByCustomerAccountIdOrderByUpdatedAtDesc(Long customerAccountId);

    long countByStatus(Quote.Status status);
}
