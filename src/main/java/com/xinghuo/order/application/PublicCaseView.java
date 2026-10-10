package com.xinghuo.order.application;

import java.time.Instant;

/** Application-owned projection for one public completed-order case. */
public record PublicCaseView(
        String maskedNo,
        String summary,
        Instant finishedAt,
        String status) {
}
