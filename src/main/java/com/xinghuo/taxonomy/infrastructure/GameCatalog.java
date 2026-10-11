package com.xinghuo.taxonomy.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="game_catalog")
public class GameCatalog {
    @Id
    @Column(length=32)
    private String code;
    @Column(nullable=false, length=80)
    private String name;
    @Column(name="name_en", length=120)
    private String nameEn;
    @Column(length=240)
    private String aliases;
    @Column(name="icon_key", length=40)
    private String iconKey;
    @Column(length=240)
    private String description;
    @Column(length=120)
    private String publisher;
    @Column(length=160)
    private String genres;
    @Column(name="logo_image", length=500)
    private String logoImage;
    @Column(name="cover_image", length=500)
    private String coverImage;
    @Column(name="popularity_score", nullable=false)
    private int popularityScore;
    @Column(name="is_featured", nullable=false)
    private boolean featured;
    @Column(name="business_status", nullable=false, length=24)
    private String businessStatus;
    @Column(name="seo_slug", length=120)
    private String seoSlug;
    @Column(name="seo_title", length=160)
    private String seoTitle;
    @Column(name="seo_description", length=300)
    private String seoDescription;
    @Column(name="is_active", nullable=false)
    private boolean active;
    @Column(name="sort_order", nullable=false)
    private int sortOrder;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static GameCatalogBuilder builder() {
        return new GameCatalogBuilder();
    }

    @Generated
    public String getCode() {
        return this.code;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getNameEn() {
        return this.nameEn;
    }

    @Generated
    public String getAliases() {
        return this.aliases;
    }

    @Generated
    public String getIconKey() {
        return this.iconKey;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public String getPublisher() {
        return this.publisher;
    }

    @Generated
    public String getGenres() {
        return this.genres;
    }

    @Generated
    public String getLogoImage() {
        return this.logoImage;
    }

    @Generated
    public String getCoverImage() {
        return this.coverImage;
    }

    @Generated
    public int getPopularityScore() {
        return this.popularityScore;
    }

    @Generated
    public boolean isFeatured() {
        return this.featured;
    }

    @Generated
    public String getBusinessStatus() {
        return this.businessStatus;
    }

    @Generated
    public String getSeoSlug() {
        return this.seoSlug;
    }

    @Generated
    public String getSeoTitle() {
        return this.seoTitle;
    }

    @Generated
    public String getSeoDescription() {
        return this.seoDescription;
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
    public void setCode(String code) {
        this.code = code;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    @Generated
    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    @Generated
    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Generated
    public void setGenres(String genres) {
        this.genres = genres;
    }

    @Generated
    public void setLogoImage(String logoImage) {
        this.logoImage = logoImage;
    }

    @Generated
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    @Generated
    public void setPopularityScore(int popularityScore) {
        this.popularityScore = popularityScore;
    }

    @Generated
    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    @Generated
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    @Generated
    public void setSeoSlug(String seoSlug) {
        this.seoSlug = seoSlug;
    }

    @Generated
    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    @Generated
    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
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
    public GameCatalog() {
    }

    @Generated
    public GameCatalog(String code, String name, String nameEn, String aliases, String iconKey, String description, String publisher, String genres, String logoImage, String coverImage, int popularityScore, boolean featured, String businessStatus, String seoSlug, String seoTitle, String seoDescription, boolean active, int sortOrder, Instant createdAt, Instant updatedAt) {
        this.code = code;
        this.name = name;
        this.nameEn = nameEn;
        this.aliases = aliases;
        this.iconKey = iconKey;
        this.description = description;
        this.publisher = publisher;
        this.genres = genres;
        this.logoImage = logoImage;
        this.coverImage = coverImage;
        this.popularityScore = popularityScore;
        this.featured = featured;
        this.businessStatus = businessStatus;
        this.seoSlug = seoSlug;
        this.seoTitle = seoTitle;
        this.seoDescription = seoDescription;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Generated
    public static class GameCatalogBuilder {
        @Generated
        private String code;
        @Generated
        private String name;
        @Generated
        private String nameEn;
        @Generated
        private String aliases;
        @Generated
        private String iconKey;
        @Generated
        private String description;
        @Generated
        private String publisher;
        @Generated
        private String genres;
        @Generated
        private String logoImage;
        @Generated
        private String coverImage;
        @Generated
        private int popularityScore;
        @Generated
        private boolean featured;
        @Generated
        private String businessStatus;
        @Generated
        private String seoSlug;
        @Generated
        private String seoTitle;
        @Generated
        private String seoDescription;
        @Generated
        private boolean active;
        @Generated
        private int sortOrder;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        GameCatalogBuilder() {
        }

        @Generated
        public GameCatalogBuilder code(String code) {
            this.code = code;
            return this;
        }

        @Generated
        public GameCatalogBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Generated
        public GameCatalogBuilder nameEn(String nameEn) {
            this.nameEn = nameEn;
            return this;
        }

        @Generated
        public GameCatalogBuilder aliases(String aliases) {
            this.aliases = aliases;
            return this;
        }

        @Generated
        public GameCatalogBuilder iconKey(String iconKey) {
            this.iconKey = iconKey;
            return this;
        }

        @Generated
        public GameCatalogBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Generated
        public GameCatalogBuilder publisher(String publisher) {
            this.publisher = publisher;
            return this;
        }

        @Generated
        public GameCatalogBuilder genres(String genres) {
            this.genres = genres;
            return this;
        }

        @Generated
        public GameCatalogBuilder logoImage(String logoImage) {
            this.logoImage = logoImage;
            return this;
        }

        @Generated
        public GameCatalogBuilder coverImage(String coverImage) {
            this.coverImage = coverImage;
            return this;
        }

        @Generated
        public GameCatalogBuilder popularityScore(int popularityScore) {
            this.popularityScore = popularityScore;
            return this;
        }

        @Generated
        public GameCatalogBuilder featured(boolean featured) {
            this.featured = featured;
            return this;
        }

        @Generated
        public GameCatalogBuilder businessStatus(String businessStatus) {
            this.businessStatus = businessStatus;
            return this;
        }

        @Generated
        public GameCatalogBuilder seoSlug(String seoSlug) {
            this.seoSlug = seoSlug;
            return this;
        }

        @Generated
        public GameCatalogBuilder seoTitle(String seoTitle) {
            this.seoTitle = seoTitle;
            return this;
        }

        @Generated
        public GameCatalogBuilder seoDescription(String seoDescription) {
            this.seoDescription = seoDescription;
            return this;
        }

        @Generated
        public GameCatalogBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        @Generated
        public GameCatalogBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        @Generated
        public GameCatalogBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public GameCatalogBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public GameCatalog build() {
            return new GameCatalog(this.code, this.name, this.nameEn, this.aliases, this.iconKey, this.description, this.publisher, this.genres, this.logoImage, this.coverImage, this.popularityScore, this.featured, this.businessStatus, this.seoSlug, this.seoTitle, this.seoDescription, this.active, this.sortOrder, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "GameCatalogBuilder(code=" + this.code + ", name=" + this.name + ", nameEn=" + this.nameEn + ", aliases=" + this.aliases + ", iconKey=" + this.iconKey + ", description=" + this.description + ", publisher=" + this.publisher + ", genres=" + this.genres + ", logoImage=" + this.logoImage + ", coverImage=" + this.coverImage + ", popularityScore=" + this.popularityScore + ", featured=" + this.featured + ", businessStatus=" + this.businessStatus + ", seoSlug=" + this.seoSlug + ", seoTitle=" + this.seoTitle + ", seoDescription=" + this.seoDescription + ", active=" + this.active + ", sortOrder=" + this.sortOrder + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}
