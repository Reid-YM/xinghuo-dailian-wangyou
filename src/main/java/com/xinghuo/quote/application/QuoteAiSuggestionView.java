package com.xinghuo.quote.application;

/** Application-owned AI-assisted lead follow-up suggestion. */
public record QuoteAiSuggestionView(
        Integer leadScore,
        String leadIntentLevel,
        String nextAction,
        String reply,
        String reason) {
}
