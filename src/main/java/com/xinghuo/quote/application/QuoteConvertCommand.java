package com.xinghuo.quote.application;

/** Application-owned command for converting a quote into an order. */
public record QuoteConvertCommand(String reason) {
}
