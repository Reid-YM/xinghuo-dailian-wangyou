package com.xinghuo.quote.application;

import java.util.List;

/** Application-owned paged quote lead result. */
public record QuoteLeadPageView(
        List<QuoteLeadView> items,
        int page,
        int size,
        long total,
        int totalPages,
        List<String> sources) {
}
