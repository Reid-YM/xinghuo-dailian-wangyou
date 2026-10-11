package com.xinghuo.catalog.infrastructure;

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

    public static enum PriceMode {
        FIXED,
        RANGE,
        QUOTE;

    }

    @Generated
    public static class ServiceItemBuilder {
        @Generated
        private Long id;
        @Generated
        private String slug;
        @Generated
        private String gameCode;
        @Generated
        private String name;
        @Generated
        private String category;
        @Generated
        private String shortDesc;
        @Generated
        private PriceMode priceMode;
        @Generated
        private BigDecimal priceFixed;
        @Generated
        private BigDecimal priceMin;
        @Generated
        private BigDecimal priceMax;
        @Generated
        private String durationText;
        @Generated
        private Status status;
        @Generated
        private String coverImage;
        @Generated
        private BigDecimal hotScore;
        @Generated
        private int manualPriority;
        @Generated
        private Long sourceEventId;
        @Generated
        private Instant publishedAt;
        @Generated
        private Instant expiresAt;
        @Generated
        private boolean active;
        @Generated
        private int sortOrder;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;
        @Generated
        private long version;

        @Generated
        ServiceItemBuilder() {
        }

        @Generated
        public ServiceItemBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public ServiceItemBuilder slug(String slug) {
            this.slug = slug;
            return this;
        }

        @Generated
        public ServiceItemBuilder gameCode(String gameCode) {
            this.gameCode = gameCode;
            return this;
        }

        @Generated
        public ServiceItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Generated
        public ServiceItemBuilder category(String category) {
            this.category = category;
            return this;
        }

        @Generated
        public ServiceItemBuilder shortDesc(String shortDesc) {
            this.shortDesc = shortDesc;
            return this;
        }

        @Generated
        public ServiceItemBuilder priceMode(PriceMode priceMode) {
            this.priceMode = priceMode;
            return this;
        }

        @Generated
        public ServiceItemBuilder priceFixed(BigDecimal priceFixed) {
            this.priceFixed = priceFixed;
            return this;
        }

        @Generated
        public ServiceItemBuilder priceMin(BigDecimal priceMin) {
            this.priceMin = priceMin;
            return this;
        }

        @Generated
        public ServiceItemBuilder priceMax(BigDecimal priceMax) {
            this.priceMax = priceMax;
            return this;
        }

        @Generated
        public ServiceItemBuilder durationText(String durationText) {
            this.durationText = durationText;
            return this;
        }

        @Generated
        public ServiceItemBuilder status(Status status) {
            this.status = status;
            return this;
        }

        @Generated
        public ServiceItemBuilder coverImage(String coverImage) {
            this.coverImage = coverImage;
            return this;
        }

        @Generated
        public ServiceItemBuilder hotScore(BigDecimal hotScore) {
            this.hotScore = hotScore;
            return this;
        }

        @Generated
        public ServiceItemBuilder manualPriority(int manualPriority) {
            this.manualPriority = manualPriority;
            return this;
        }

        @Generated
        public ServiceItemBuilder sourceEventId(Long sourceEventId) {
            this.sourceEventId = sourceEventId;
            return this;
        }

        @Generated
        public ServiceItemBuilder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        @Generated
        public ServiceItemBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        @Generated
        public ServiceItemBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        @Generated
        public ServiceItemBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        @Generated
        public ServiceItemBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public ServiceItemBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public ServiceItemBuilder version(long version) {
            this.version = version;
            return this;
        }

        @Generated
        public ServiceItem build() {
            return new ServiceItem(this.id, this.slug, this.gameCode, this.name, this.category, this.shortDesc, this.priceMode, this.priceFixed, this.priceMin, this.priceMax, this.durationText, this.status, this.coverImage, this.hotScore, this.manualPriority, this.sourceEventId, this.publishedAt, this.expiresAt, this.active, this.sortOrder, this.createdAt, this.updatedAt, this.version);
        }

        @Generated
        public String toString() {
            return "ServiceItemBuilder(id=" + this.id + ", slug=" + this.slug + ", gameCode=" + this.gameCode + ", name=" + this.name + ", category=" + this.category + ", shortDesc=" + this.shortDesc + ", priceMode=" + String.valueOf(this.priceMode) + ", priceFixed=" + String.valueOf(this.priceFixed) + ", priceMin=" + String.valueOf(this.priceMin) + ", priceMax=" + String.valueOf(this.priceMax) + ", durationText=" + this.durationText + ", status=" + String.valueOf(this.status) + ", coverImage=" + this.coverImage + ", hotScore=" + String.valueOf(this.hotScore) + ", manualPriority=" + this.manualPriority + ", sourceEventId=" + this.sourceEventId + ", publishedAt=" + String.valueOf(this.publishedAt) + ", expiresAt=" + String.valueOf(this.expiresAt) + ", active=" + this.active + ", sortOrder=" + this.sortOrder + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ", version=" + this.version + ")";
        }
    }

    public static enum Status {
        AVAILABLE,
        BUSY,
        OFFLINE;

    }
}
