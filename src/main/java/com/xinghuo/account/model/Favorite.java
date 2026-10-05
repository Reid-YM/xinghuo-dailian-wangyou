package com.xinghuo.account.model;

import com.xinghuo.account.model.Comment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 收藏。target_type 与 Comment 共用同一套目标类型。 */
@Entity
@Table(name = "user_favorite",
        uniqueConstraints = @UniqueConstraint(name = "uk_favorite",
                columnNames = {"user_id", "target_type", "target_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private Comment.TargetType targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Generated
    public static class FavoriteBuilder {
        @Generated
        private Long id;
        @Generated
        private Long userId;
        @Generated
        private Comment.TargetType targetType;
        @Generated
        private String targetId;
        @Generated
        private Instant createdAt;

        @Generated
        FavoriteBuilder() {
        }

        @Generated
        public FavoriteBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public FavoriteBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public FavoriteBuilder targetType(Comment.TargetType targetType) {
            this.targetType = targetType;
            return this;
        }

        @Generated
        public FavoriteBuilder targetId(String string) {
            this.targetId = string;
            return this;
        }

        @Generated
        public FavoriteBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public Favorite build() {
            return new Favorite(this.id, this.userId, this.targetType, this.targetId, this.createdAt);
        }

        @Generated
        public String toString() {
            return "FavoriteBuilder(id=" + this.id + ", userId=" + this.userId + ", targetType=" + String.valueOf(this.targetType) + ", targetId=" + this.targetId + ", createdAt=" + String.valueOf(this.createdAt) + ")";
        }
    }
}
