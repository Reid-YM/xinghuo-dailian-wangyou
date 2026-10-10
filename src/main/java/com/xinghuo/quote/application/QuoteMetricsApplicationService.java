package com.xinghuo.quote.application;

import org.springframework.stereotype.Service;

@Service
public class QuoteMetricsApplicationService {
    private final QuoteMetricsPort port;

    public QuoteMetricsApplicationService(QuoteMetricsPort port) {
        this.port = port;
    }

    public long countNew() {
        return port.countNew();
    }
}
