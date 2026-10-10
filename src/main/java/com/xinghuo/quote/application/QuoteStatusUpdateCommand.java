package com.xinghuo.quote.application;

/** Application-owned command for administrative quote status changes. */
public record QuoteStatusUpdateCommand(String status, String reason) {
}
