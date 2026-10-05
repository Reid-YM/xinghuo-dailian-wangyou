package com.xinghuo.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 评论举报。同一人对同一条评论只能举报一次（唯一键保证）。 */
@Entity
@Table(name = "user_comment_report")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 120)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Generated
    public static class CommentReportBuilder {
        @Generated
        private Long id;
        @Generated
        private Long commentId;
        @Generated
        private Long userId;
        @Generated
        private String reason;
        @Generated
        private Instant createdAt;

        @Generated
        CommentReportBuilder() {
        }

        @Generated
        public CommentReportBuilder id(Long l) {
            this.id = l;
            return this;
        }

        @Generated
        public CommentReportBuilder commentId(Long l) {
            this.commentId = l;
            return this;
        }

        @Generated
        public CommentReportBuilder userId(Long l) {
            this.userId = l;
            return this;
        }

        @Generated
        public CommentReportBuilder reason(String string) {
            this.reason = string;
            return this;
        }

        @Generated
        public CommentReportBuilder createdAt(Instant instant) {
            this.createdAt = instant;
            return this;
        }

        @Generated
        public CommentReport build() {
            return new CommentReport(this.id, this.commentId, this.userId, this.reason, this.createdAt);
        }

        @Generated
        public String toString() {
            return "CommentReportBuilder(id=" + this.id + ", commentId=" + this.commentId + ", userId=" + this.userId + ", reason=" + this.reason + ", createdAt=" + String.valueOf(this.createdAt) + ")";
        }
    }
}
