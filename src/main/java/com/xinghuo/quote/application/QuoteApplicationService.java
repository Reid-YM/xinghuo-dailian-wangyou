package com.xinghuo.quote.application;

import org.springframework.stereotype.Service;

/** Application entry point for public quote submission. */
@Service
public class QuoteApplicationService {
    private final QuoteLegacyPort legacy;

    public QuoteApplicationService(QuoteLegacyPort legacy) {
        this.legacy = legacy;
    }

    public QuoteSubmissionResult create(QuoteSubmissionCommand command) {
        return legacy.create(command);
    }
}
