package com.xinghuo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Generated;

@Entity
@Table(name="admin_test_feedback")
public class AdminTestFeedback {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=120)
    private String title;
    @Column(name="page_path", length=240)
    private String pagePath;
    @Column(nullable=false, columnDefinition="TEXT")
    private String content;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=16)
    private Priority priority;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=24)
    private Status status;
    @Column(name="operator_note", length=1000)
    private String operatorNote;
    @Column(name="attachment_asset_id")
    private UUID attachmentAssetId;
    @Column(name="created_by", nullable=false, length=100)
    private String createdBy;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static AdminTestFeedbackBuilder builder() {
        return new AdminTestFeedbackBuilder();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getTitle() {
        return this.title;
    }

    @Generated
    public String getPagePath() {
        return this.pagePath;
    }

    @Generated
    public String getContent() {
        return this.content;
    }

    @Generated
    public Priority getPriority() {
        return this.priority;
    }

    @Generated
    public Status getStatus() {
        return this.status;
    }

    @Generated
    public String getOperatorNote() {
        return this.operatorNote;
    }

    @Generated
    public UUID getAttachmentAssetId() {
        return this.attachmentAssetId;
    }

    @Generated
    public String getCreatedBy() {
        return this.createdBy;
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
    public void setTitle(String title) {
        this.title = title;
    }

    @Generated
    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    @Generated
    public void setContent(String content) {
        this.content = content;
    }

    @Generated
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    @Generated
    public void setStatus(Status status) {
        this.status = status;
    }

    @Generated
    public void setOperatorNote(String operatorNote) {
        this.operatorNote = operatorNote;
    }

    @Generated
    public void setAttachmentAssetId(UUID attachmentAssetId) {
        this.attachmentAssetId = attachmentAssetId;
    }

    @Generated
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
    public AdminTestFeedback() {
    }

    @Generated
    public AdminTestFeedback(Long id, String title, String pagePath, String content, Priority priority, Status status, String operatorNote, UUID attachmentAssetId, String createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.pagePath = pagePath;
        this.content = content;
        this.priority = priority;
        this.status = status;
        this.operatorNote = operatorNote;
        this.attachmentAssetId = attachmentAssetId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Generated
    public static class AdminTestFeedbackBuilder {
        @Generated
        private Long id;
        @Generated
        private String title;
        @Generated
        private String pagePath;
        @Generated
        private String content;
        @Generated
        private Priority priority;
        @Generated
        private Status status;
        @Generated
        private String operatorNote;
        @Generated
        private UUID attachmentAssetId;
        @Generated
        private String createdBy;
        @Generated
        private Instant createdAt;
        @Generated
        private Instant updatedAt;

        @Generated
        AdminTestFeedbackBuilder() {
        }

        @Generated
        public AdminTestFeedbackBuilder id(Long id) {
            this.id = id;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder title(String title) {
            this.title = title;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder pagePath(String pagePath) {
            this.pagePath = pagePath;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder content(String content) {
            this.content = content;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder status(Status status) {
            this.status = status;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder operatorNote(String operatorNote) {
            this.operatorNote = operatorNote;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder attachmentAssetId(UUID attachmentAssetId) {
            this.attachmentAssetId = attachmentAssetId;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @Generated
        public AdminTestFeedbackBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @Generated
        public AdminTestFeedback build() {
            return new AdminTestFeedback(this.id, this.title, this.pagePath, this.content, this.priority, this.status, this.operatorNote, this.attachmentAssetId, this.createdBy, this.createdAt, this.updatedAt);
        }

        @Generated
        public String toString() {
            return "AdminTestFeedbackBuilder(id=" + this.id + ", title=" + this.title + ", pagePath=" + this.pagePath + ", content=" + this.content + ", priority=" + String.valueOf(this.priority) + ", status=" + String.valueOf(this.status) + ", operatorNote=" + this.operatorNote + ", attachmentAssetId=" + String.valueOf(this.attachmentAssetId) + ", createdBy=" + this.createdBy + ", createdAt=" + String.valueOf(this.createdAt) + ", updatedAt=" + String.valueOf(this.updatedAt) + ")";
        }
    }

    public static enum Priority {
        LOW,
        NORMAL,
        HIGH,
        URGENT;

    }

    public static enum Status {
        NEW,
        ACCEPTED,
        TESTING,
        DONE,
        PAUSED;

    }
}

