package com.xinghuo.taxonomy.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Generated;

@Entity
@Table(name="service_categories", uniqueConstraints={@UniqueConstraint(name="uk_service_category_game_code", columnNames={"game_code", "code"})})
public class ServiceCategoryCatalog {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="game_code", nullable=false, length=32)
    private String gameCode;
    @Column(nullable=false, length=32)
    private String code;
    @Column(nullable=false, length=80)
    private String name;
    @Column(name="icon_key", length=40)
    private String iconKey;
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
    public static ServiceCategoryCatalogBuilder builder() {
        return new ServiceCategoryCatalogBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getGameCode() {
        return this.gameCode;
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
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
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
    public ServiceCategoryCatalog() {
    }

    @Generated
    public ServiceCategoryCatalog(Long id, String gameCode, String code, String name, String iconKey, String description, boolean active, int sortOrder, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.gameCode = gameCode;
        this.code = code;
        this.name = name;
        this.iconKey = iconKey;
        this.description = description;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Generated
    public static class ServiceCategoryCatalogBuilder {
        @Generated
        private Long id;
        @Generated
        private String gameCode;
        @Generated
        private String code;
        @Generated
        private String name;
        @Generated
        private String iconKey;
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
        ServiceCategoryCatalogBuilder() {
        }

        @Generated
        public ServiceCategoryCatalogBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder gameCode(String gameCode) {
            this.gameCode = gameCode;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder code(String code) {
            this.code = code;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder name(String name) {
            this.name = name;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder iconKey(String iconKey) {
            this.iconKey = iconKey;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder description(String description) {
            this.description = description;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public ServiceCategoryCatalogBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public ServiceCategoryCatalog build() {
            return new ServiceCategoryCatalog(this.id, this.gameCode, this.code, this.name, this.iconKey, this.description, this.active, this.sortOrder, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "ServiceCategoryCatalogBuilder(id=" + this.id + ", gameCode=" + this.gameCode + ", code=" + this.code + ", name=" + this.name + ", iconKey=" + this.iconKey + ", description=" + this.description + ", active=" + this.active + ", sortOrder=" + this.sortOrder + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}
