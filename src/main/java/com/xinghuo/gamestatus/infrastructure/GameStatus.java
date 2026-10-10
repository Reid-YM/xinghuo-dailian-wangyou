package com.xinghuo.gamestatus.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import lombok.Generated;

@Entity
@Table(name="game_status")
public class GameStatus {
    @Id
    @Column(name="game_code", length=32)
    private String gameCode;
    @Enumerated(value=EnumType.STRING)
    @Column(name="status_code", nullable=false, length=16)
    private StatusCode statusCode;
    @Column(name="public_label", nullable=false, length=80)
    private String publicLabel;
    @Column(name="public_detail", length=240)
    private String publicDetail;
    @Column(name="cta_label", length=40)
    private String ctaLabel;
    @Column(name="effective_at")
    private Instant effectiveAt;
    @Column(name="updated_by", length=80)
    private String updatedBy;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;
    @Version
    private long version;

    @Generated
    public static GameStatusBuilder builder() {
        return new GameStatusBuilder();
    }

    @Generated
    public String getGameCode() {
        return this.gameCode;
    }

    @Generated
    public StatusCode getStatusCode() {
        return this.statusCode;
    }

    @Generated
    public String getPublicLabel() {
        return this.publicLabel;
    }

    @Generated
    public String getPublicDetail() {
        return this.publicDetail;
    }

    @Generated
    public String getCtaLabel() {
        return this.ctaLabel;
    }

    @Generated
    public Instant getEffectiveAt() {
        return this.effectiveAt;
    }

    @Generated
    public String getUpdatedBy() {
        return this.updatedBy;
    }

    @Generated
    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public long getVersion() {
        return this.version;
    }

    @Generated
    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    @Generated
    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    @Generated
    public void setPublicLabel(String publicLabel) {
        this.publicLabel = publicLabel;
    }

    @Generated
    public void setPublicDetail(String publicDetail) {
        this.publicDetail = publicDetail;
    }

    @Generated
    public void setCtaLabel(String ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    @Generated
    public void setEffectiveAt(Instant effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    @Generated
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Generated
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public void setVersion(long version) {
        this.version = version;
    }

    @Generated
    public GameStatus() {
    }

    @Generated
    public GameStatus(String gameCode, StatusCode statusCode, String publicLabel, String publicDetail, String ctaLabel, Instant effectiveAt, String updatedBy, Instant updatedAt, long version) {
        this.gameCode = gameCode;
        this.statusCode = statusCode;
        this.publicLabel = publicLabel;
        this.publicDetail = publicDetail;
        this.ctaLabel = ctaLabel;
        this.effectiveAt = effectiveAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    @Generated
    public static class GameStatusBuilder {
        @Generated
        private String gameCode;
        @Generated
        private StatusCode statusCode;
        @Generated
        private String publicLabel;
        @Generated
        private String publicDetail;
        @Generated
        private String ctaLabel;
        @Generated
        private Instant effectiveAt;
        @Generated
        private String updatedBy;
        @Generated
        private Instant updatedAt;
        @Generated
        private long version;

        @Generated
        GameStatusBuilder() {
        }

        @Generated
        public GameStatusBuilder gameCode(String gameCode) {
            this.gameCode = gameCode;
            return this;
        }

        @Generated
        public GameStatusBuilder statusCode(StatusCode statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Generated
        public GameStatusBuilder publicLabel(String publicLabel) {
            this.publicLabel = publicLabel;
            return this;
        }

        @Generated
        public GameStatusBuilder publicDetail(String publicDetail) {
            this.publicDetail = publicDetail;
            return this;
        }

        @Generated
        public GameStatusBuilder ctaLabel(String ctaLabel) {
            this.ctaLabel = ctaLabel;
            return this;
        }

        @Generated
        public GameStatusBuilder effectiveAt(Instant effectiveAt) {
            this.effectiveAt = effectiveAt;
            return this;
        }

        @Generated
        public GameStatusBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        @Generated
        public GameStatusBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public GameStatusBuilder version(long version) {
            this.version = version;
            return this;
        }

        @Generated
        public GameStatus build() {
            return new GameStatus(this.gameCode, this.statusCode, this.publicLabel, this.publicDetail, this.ctaLabel, this.effectiveAt, this.updatedBy, this.updatedAt, this.version);
        }

        @Generated
        public String toString() {
            return "GameStatusBuilder(gameCode=" + this.gameCode + ", statusCode=" + String.valueOf(this.statusCode) + ", publicLabel=" + this.publicLabel + ", publicDetail=" + this.publicDetail + ", ctaLabel=" + this.ctaLabel + ", effectiveAt=" + String.valueOf(this.effectiveAt) + ", updatedBy=" + this.updatedBy + ", updatedAt=" + String.valueOf(this.updatedAt) + ", version=" + this.version + ")";
        }
    }

    public static enum StatusCode {
        PREORDER,
        OPEN,
        PAUSED,
        MAINTENANCE,
        LIMITED;

        public boolean acceptingOrders() {
            return this == OPEN;
        }

        public boolean preorder() {
            return this == PREORDER;
        }

        public static StatusCode parse(String raw) {
            String v;
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("\u72b6\u6001\u4e0d\u80fd\u4e3a\u7a7a");
            }
            return switch (v = raw.trim().toUpperCase(Locale.ROOT)) {
                case "BUSY", "RESERVATION", "PREHEAT" -> PREORDER;
                case "SUSPENDED", "CLOSED" -> PAUSED;
                default -> StatusCode.valueOf(v);
            };
        }
    }
}

