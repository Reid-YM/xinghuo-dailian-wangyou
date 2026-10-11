package com.xinghuo.quote.infrastructure;

import com.xinghuo.quote.application.QuoteMetricsPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class QuoteMetricsJpaAdapter implements QuoteMetricsPort {
    private final QuoteRepository repo;

    public QuoteMetricsJpaAdapter(QuoteRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public long countNew() {
        return repo.countByStatus(Quote.Status.NEW);
    }
}
