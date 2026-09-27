/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.GameCatalog
 *  com.xinghuo.model.GameCatalog$GameCatalogBuilder
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.GameCatalog;
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
}

