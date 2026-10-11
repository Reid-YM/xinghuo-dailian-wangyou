package com.xinghuo.repository;

import com.xinghuo.model.Quote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface QuoteRepository
extends JpaRepository<Quote, UUID>,
JpaSpecificationExecutor<Quote> {
    @Query(value="select distinct q.utmSource from Quote q where q.utmSource is not null order by q.utmSource")
    public List<String> distinctUtmSources();

    public Optional<Quote> findByIdempotencyKey(String var1);

    public Optional<Quote> findByBusinessNo(String var1);

    public List<Quote> findTop100ByOrderByCreatedAtDesc();

    public List<Quote> findTop50ByCustomerAccountIdOrderByUpdatedAtDesc(Long var1);

    public long countByStatus(Quote.Status var1);
}

