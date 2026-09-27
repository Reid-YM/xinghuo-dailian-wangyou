/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.AdminTestFeedback
 *  com.xinghuo.model.AdminTestFeedback$AdminTestFeedbackBuilder
 *  com.xinghuo.model.AdminTestFeedback$Priority
 *  com.xinghuo.model.AdminTestFeedback$Status
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.AdminTestFeedback;
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
}

