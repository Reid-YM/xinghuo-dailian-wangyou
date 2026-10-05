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
 * 用户攻略投稿。
 *
 * <p>只在审核通过后才由后台写入 game_guides，本表不复制攻略的正文结构 ——
 * 否则同一篇内容会出现两份真相，后续编辑必然分叉。
 */
@Entity
@Table(name = "user_submission")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "game_code", nullable = false, length = 32)
    private String gameCode;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(name = "content_md", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String contentMd;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubmissionStatus status;

    @Column(name = "reviewer_name", length = 80)
    private String reviewerName;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "published_slug", length = 140)
    private String publishedSlug;

    @Column(name = "published_at")
    private Instant publishedAt;

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
            status = SubmissionStatus.SUBMITTED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** 用户能否自行撤回：只有还没进入审核的结果态才可撤回。 */
    public boolean isWithdrawable() {
        return status == SubmissionStatus.SUBMITTED || status == SubmissionStatus.IN_REVIEW;
    }

    public enum SubmissionStatus {
        SUBMITTED,
        IN_REVIEW,
        APPROVED,
        REJECTED,
        PUBLISHED,
        WITHDRAWN
    }

    @Generated
    public static class SubmissionBuilder {
        @Generated
        private Long id;
        @Generated
        private Long userId;
        @Generated
        private String gameCode;
        @Generated
        private String title;
        @Generated
        private String summary;
        @Generated
        private String contentMd;
        @Generated
        private String coverImage;
        @Generated
        private SubmissionStatus status;
        @Generated
        private String reviewerName;
        @Generated
        private String reviewNote;
        @Generated
        private String publishedSlug;
        @Generated
        private Instant publishedAt;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        SubmissionBuilder() {
        }

        @Generated
        public SubmissionBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public SubmissionBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public SubmissionBuilder gameCode(String string) {
            this.gameCode = string;
            return this;
        }

        @Generated
        public SubmissionBuilder title(String string) {
            this.title = string;
            return this;
        }

        @Generated
        public SubmissionBuilder summary(String string) {
            this.summary = string;
            return this;
        }

        @Generated
        public SubmissionBuilder contentMd(String string) {
            this.contentMd = string;
            return this;
        }

        @Generated
        public SubmissionBuilder coverImage(String string) {
            this.coverImage = string;
            return this;
        }

        @Generated
        public SubmissionBuilder status(SubmissionStatus submissionStatus) {
            this.status = submissionStatus;
            return this;
        }

        @Generated
        public SubmissionBuilder reviewerName(String string) {
            this.reviewerName = string;
            return this;
        }

        @Generated
        public SubmissionBuilder reviewNote(String string) {
            this.reviewNote = string;
            return this;
        }

        @Generated
        public SubmissionBuilder publishedSlug(String string) {
            this.publishedSlug = string;
            return this;
        }

        @Generated
        public SubmissionBuilder publishedAt(Instant instant) {
            this.publishedAt = instant;
            return this;
        }

        @Generated
        public SubmissionBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public SubmissionBuilder updatedAt(Instant instant) {
            this.updatedAt = instant;
            return this;
        }

        @Generated
        public Submission build() {
            return new Submission(this.id, this.userId, this.gameCode, this.title, this.summary, this.contentMd, this.coverImage, this.status, this.reviewerName, this.reviewNote, this.publishedSlug, this.publishedAt, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "SubmissionBuilder(id=" + this.id + ", userId=" + this.userId + ", gameCode=" + this.gameCode + ", title=" + this.title + ", summary=" + this.summary + ", contentMd=" + this.contentMd + ", coverImage=" + this.coverImage + ", status=" + String.valueOf(this.status) + ", reviewerName=" + this.reviewerName + ", reviewNote=" + this.reviewNote + ", publishedSlug=" + this.publishedSlug + ", publishedAt=" + String.valueOf(this.publishedAt) + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }
}
