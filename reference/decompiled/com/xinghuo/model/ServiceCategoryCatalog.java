/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.ServiceCategoryCatalog
 *  com.xinghuo.model.ServiceCategoryCatalog$ServiceCategoryCatalogBuilder
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  jakarta.persistence.UniqueConstraint
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.ServiceCategoryCatalog;
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
}

