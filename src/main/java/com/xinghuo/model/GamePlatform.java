package com.xinghuo.model;

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
@Table(name="game_platforms", uniqueConstraints={@UniqueConstraint(name="uk_game_platform", columnNames={"game_code", "platform_code"})})
public class GamePlatform {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="game_code", nullable=false, length=32)
    private String gameCode;
    @Column(name="platform_code", nullable=false, length=32)
    private String platformCode;
    @Column(name="is_active", nullable=false)
    private boolean active;
    @Column(name="sort_order", nullable=false)
    private int sortOrder;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static GamePlatformBuilder builder() {
        return new GamePlatformBuilder();
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
    public String getPlatformCode() {
        return this.platformCode;
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
    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
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
    public GamePlatform() {
    }

    @Generated
    public GamePlatform(Long id, String gameCode, String platformCode, boolean active, int sortOrder, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.gameCode = gameCode;
        this.platformCode = platformCode;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Generated
    public static class GamePlatformBuilder {
        @Generated
        private Long id;
        @Generated
        private String gameCode;
        @Generated
        private String platformCode;
        @Generated
        private boolean active;
        @Generated
        private int sortOrder;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        GamePlatformBuilder() {
        }

        @Generated
        public GamePlatformBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public GamePlatformBuilder gameCode(String gameCode) {
            this.gameCode = gameCode;
            return this;
        }

        @Generated
        public GamePlatformBuilder platformCode(String platformCode) {
            this.platformCode = platformCode;
            return this;
        }

        @Generated
        public GamePlatformBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        @Generated
        public GamePlatformBuilder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        @Generated
        public GamePlatformBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public GamePlatformBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public GamePlatform build() {
            return new GamePlatform(this.id, this.gameCode, this.platformCode, this.active, this.sortOrder, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "GamePlatformBuilder(id=" + this.id + ", gameCode=" + this.gameCode + ", platformCode=" + this.platformCode + ", active=" + this.active + ", sortOrder=" + this.sortOrder + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}

