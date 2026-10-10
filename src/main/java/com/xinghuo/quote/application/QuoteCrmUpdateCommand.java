package com.xinghuo.quote.application;

/** Application-owned command for CRM follow-up changes. */
public record QuoteCrmUpdateCommand(
        Integer leadScore,
        String leadIntentLevel,
        String nextAction,
        String followUpNote,
        String reason) {
}
