/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.GameStatus
 *  com.xinghuo.model.GameStatus$GameStatusBuilder
 *  com.xinghuo.model.GameStatus$StatusCode
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  jakarta.persistence.Version
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.GameStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
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
}

