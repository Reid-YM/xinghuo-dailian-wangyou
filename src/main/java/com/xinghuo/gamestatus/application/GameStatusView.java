package com.xinghuo.gamestatus.application;

import java.time.Instant;

/** Application-owned game-status projection. */
public record GameStatusView(
        String gameCode,
        GameStatusCode statusCode,
        String publicLabel,
        String publicDetail,
        String ctaLabel,
        Instant effectiveAt,
        String updatedBy,
        Instant updatedAt,
        long version) {

    public String getGameCode() { return gameCode; }
    public GameStatusCode getStatusCode() { return statusCode; }
    public String getPublicLabel() { return publicLabel; }
    public String getPublicDetail() { return publicDetail; }
    public String getCtaLabel() { return ctaLabel; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
