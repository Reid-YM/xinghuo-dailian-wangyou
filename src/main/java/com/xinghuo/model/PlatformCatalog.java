package com.xinghuo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="platform_catalog")
public class PlatformCatalog {
    @Id
    @Column(length=32)
    private String code;
    @Column(nullable=false, length=80)
    private String name;
    @Column(name="icon_key", length=40)
    private String iconKey;
    @Column(name="platform_group", length=32)
    private String platformGroup;
    @Column(length=240)
    private String description;
    @Column(name="is_active", nullable=false)
    private boolean active;
    @Column(name="sort_order", nullable=false)
    private int sortOrder;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static PlatformCatalogBuilder builder() {
        return new PlatformCatalogBuilder();
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
    public String getIconKey() {
        return this.iconKey;
    }

    @Generated
    public String getPlatformGroup() {
        return this.platformGroup;
    }

    @Generated
    public String getDescription() {
        return this.description;
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
    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    @Generated
    public void setPlatformGroup(String platformGroup) {
        this.platformGroup = platformGroup;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
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
    public PlatformCatalog() {
    }

    @Generated
    public PlatformCatalog(String code, String name, String iconKey, String platformGroup, String description, boolean active, int sortOrder, Instant createdAt, Instant updatedAt) {
        this.code = code;
        this.name = name;
        this.iconKey = iconKey;
        this.platformGroup = platformGroup;
        this.description = description;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Generated
    public static class PlatformCatalogBuilder {
        @Generated
        private String code;
        @Generated
        private String name;
        @Generated
        private String iconKey;
        @Generated
        private String platformGroup;
        @Generated
        private String description;
        @Generated
        private boolean active;
        @Generated
        private int sortOrder;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        PlatformCatalogBuilder() {
        }

        @Generated
        public PlatformCatalogBuilder code(String code) {
            this.code = code;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder iconKey(String iconKey) {
            this.iconKey = iconKey;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder platformGroup(String platformGroup) {
            this.platformGroup = platformGroup;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public PlatformCatalogBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public PlatformCatalog build() {
            return new PlatformCatalog(this.code, this.name, this.iconKey, this.platformGroup, this.description, this.active, this.sortOrder, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "PlatformCatalogBuilder(code=" + this.code + ", name=" + this.name + ", iconKey=" + this.iconKey + ", platformGroup=" + this.platformGroup + ", description=" + this.description + ", active=" + this.active + ", sortOrder=" + this.sortOrder + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}

