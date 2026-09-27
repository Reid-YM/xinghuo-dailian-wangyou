/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.ServiceItem
 *  com.xinghuo.model.ServiceItem$PriceMode
 *  com.xinghuo.model.ServiceItem$ServiceItemBuilder
 *  com.xinghuo.model.ServiceItem$Status
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  jakarta.persistence.Version
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.ServiceItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="services")
public class ServiceItem {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=100)
    private String slug;
    @Column(name="game_code", nullable=false, length=32)
    private String gameCode;
    @Column(nullable=false, length=120)
    private String name;
    @Column(nullable=false, length=32)
    private String category;
    @Column(name="short_desc", nullable=false, length=240)
    private String shortDesc;
    @Enumerated(value=EnumType.STRING)
    @Column(name="price_mode", nullable=false, length=16)
    private PriceMode priceMode;
    @Column(name="price_fixed", precision=10, scale=2)
    private BigDecimal priceFixed;
    @Column(name="price_min", precision=10, scale=2)
    private BigDecimal priceMin;
    @Column(name="price_max", precision=10, scale=2)
    private BigDecimal priceMax;
    @Column(name="duration_text", nullable=false, length=100)
    private String durationText;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=16)
    private Status status;
    @Column(name="cover_image", length=500)
    private String coverImage;
    @Column(name="hot_score", nullable=false, precision=8, scale=2)
    private BigDecimal hotScore;
    @Column(name="manual_priority", nullable=false)
    private int manualPriority;
    @Column(name="source_event_id")
    private Long sourceEventId;
    @Column(name="published_at")
    private Instant publishedAt;
    @Column(name="expires_at")
    private Instant expiresAt;
    @Column(name="is_active", nullable=false)
    private boolean active;
    @Column(name="sort_order", nullable=false)
    private int sortOrder;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;
    @Version
    private long version;

    @Generated
    public static ServiceItemBuilder builder() {
        return new ServiceItemBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getSlug() {
        return this.slug;
    }

    @Generated
    public String getGameCode() {
        return this.gameCode;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getCategory() {
        return this.category;
    }

    @Generated
    public String getShortDesc() {
        return this.shortDesc;
    }

    @Generated
    public PriceMode getPriceMode() {
        return this.priceMode;
    }

    @Generated
    public BigDecimal getPriceFixed() {
        return this.priceFixed;
    }

    @Generated
    public BigDecimal getPriceMin() {
        return this.priceMin;
    }

    @Generated
    public BigDecimal getPriceMax() {
        return this.priceMax;
    }

    @Generated
    public String getDurationText() {
        return this.durationText;
    }

    @Generated
    public Status getStatus() {
        return this.status;
    }

    @Generated
    public String getCoverImage() {
        return this.coverImage;
    }

    @Generated
    public BigDecimal getHotScore() {
        return this.hotScore;
    }

    @Generated
    public int getManualPriority() {
        return this.manualPriority;
    }

    @Generated
    public Long getSourceEventId() {
        return this.sourceEventId;
    }

    @Generated
    public Instant getPublishedAt() {
        return this.publishedAt;
    }

    @Generated
    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    @Generated
    public boolean isActive() {
        return this.active;
    }

    @Generated
    public int getSortOrder() {
        return this.sortOrder;
    }

    @Generated
    public Instant getCreatedAt() {
        return this.createdAt;
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
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Generated
    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setCategory(String category) {
        this.category = category;
    }

    @Generated
    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    @Generated
    public void setPriceMode(PriceMode priceMode) {
        this.priceMode = priceMode;
    }

    @Generated
    public void setPriceFixed(BigDecimal priceFixed) {
        this.priceFixed = priceFixed;
    }

    @Generated
    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    @Generated
    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    @Generated
    public void setDurationText(String durationText) {
        this.durationText = durationText;
    }

    @Generated
    public void setStatus(Status status) {
        this.status = status;
    }

    @Generated
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    @Generated
    public void setHotScore(BigDecimal hotScore) {
        this.hotScore = hotScore;
    }

    @Generated
    public void setManualPriority(int manualPriority) {
        this.manualPriority = manualPriority;
    }

    @Generated
    public void setSourceEventId(Long sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    @Generated
    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    @Generated
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Generated
    public void setActive(boolean active) {
        this.active = active;
    }

    @Generated
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Generated
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
    public ServiceItem() {
    }

    @Generated
    public ServiceItem(Long id, String slug, String gameCode, String name, String category, String shortDesc, PriceMode priceMode, BigDecimal priceFixed, BigDecimal priceMin, BigDecimal priceMax, String durationText, Status status, String coverImage, BigDecimal hotScore, int manualPriority, Long sourceEventId, Instant publishedAt, Instant expiresAt, boolean active, int sortOrder, Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.slug = slug;
        this.gameCode = gameCode;
        this.name = name;
        this.category = category;
        this.shortDesc = shortDesc;
        this.priceMode = priceMode;
        this.priceFixed = priceFixed;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.durationText = durationText;
        this.status = status;
        this.coverImage = coverImage;
        this.hotScore = hotScore;
        this.manualPriority = manualPriority;
        this.sourceEventId = sourceEventId;
        this.publishedAt = publishedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }
}

