package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 评论。
 *
 * <p>content 只存纯文本，渲染端负责转义 —— 服务端不接受任何用户 HTML，
 * 这样即使某处渲染忘了转义，也不会变成存储型 XSS。
 */
@Entity
@Table(name = "user_comment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false, length = 64)
    private String targetId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CommentStatus status;

    @Column(name = "audit_note", length = 300)
    private String auditNote;

    @Column(name = "reviewed_by_name", length = 80)
    private String reviewedByName;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = CommentStatus.PUBLISHED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isVisible() {
        return status == CommentStatus.PUBLISHED;
    }

    public enum TargetType {
        GUIDE,
        GAME,
        SERVICE
    }

    public enum CommentStatus {
        PENDING,
        PUBLISHED,
        REJECTED,
        DELETED
    }

    @Generated
    public static class CommentBuilder {
        @Generated
        private Long id;
        @Generated
        private Long userId;
        @Generated
        private TargetType targetType;
        @Generated
        private String targetId;
        @Generated
        private Long parentId;
        @Generated
        private String content;
        @Generated
        private CommentStatus status;
        @Generated
        private String auditNote;
        @Generated
        private String reviewedByName;
        @Generated
        private Instant reviewedAt;
        @Generated
        private int reportCount;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        CommentBuilder() {
        }

        @Generated
        public CommentBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public CommentBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public CommentBuilder targetType(TargetType targetType) {
            this.targetType = targetType;
            return this;
        }

        @Generated
        public CommentBuilder targetId(String string) {
            this.targetId = string;
            return this;
        }

        @Generated
        public CommentBuilder parentId(Long l) {
            this.parentId = l;
            return this;
        }

        @Generated
        public CommentBuilder content(String string) {
            this.content = string;
            return this;
        }

        @Generated
        public CommentBuilder status(CommentStatus commentStatus) {
            this.status = commentStatus;
            return this;
        }

        @Generated
        public CommentBuilder auditNote(String string) {
            this.auditNote = string;
            return this;
        }

        @Generated
        public CommentBuilder reviewedByName(String string) {
            this.reviewedByName = string;
            return this;
        }

        @Generated
        public CommentBuilder reviewedAt(Instant instant) {
            this.reviewedAt = instant;
            return this;
        }

        @Generated
        public CommentBuilder reportCount(int n) {
            this.reportCount = n;
            return this;
        }

        @Generated
        public CommentBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public CommentBuilder updatedAt(Instant instant) {
            this.updatedAt = instant;
            return this;
        }

        @Generated
        public Comment build() {
            return new Comment(this.id, this.userId, this.targetType, this.targetId, this.parentId, this.content, this.status, this.auditNote, this.reviewedByName, this.reviewedAt, this.reportCount, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "CommentBuilder(id=" + this.id + ", userId=" + this.userId + ", targetType=" + String.valueOf(this.targetType) + ", targetId=" + this.targetId + ", parentId=" + this.parentId + ", content=" + this.content + ", status=" + String.valueOf(this.status) + ", auditNote=" + this.auditNote + ", reviewedByName=" + this.reviewedByName + ", reviewedAt=" + String.valueOf(this.reviewedAt) + ", reportCount=" + this.reportCount + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}
