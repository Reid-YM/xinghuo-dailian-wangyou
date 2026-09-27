/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.CustomerOrder
 *  com.xinghuo.model.CustomerOrder$CustomerOrderBuilder
 *  com.xinghuo.model.CustomerOrder$Status
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  jakarta.persistence.Version
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.CustomerOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Generated;

@Entity
@Table(name="customer_orders")
public class CustomerOrder {
    @Id
    private UUID id;
    @Column(name="order_no", nullable=false, unique=true, length=40)
    private String orderNo;
    @Column(name="business_no", nullable=false, unique=true, length=20)
    private String businessNo;
    @Column(name="source_quote_id", unique=true)
    private UUID sourceQuoteId;
    @Column(name="customer_account_id")
    private Long customerAccountId;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=32)
    private Status status;
    @Column(name="progress_percent")
    private Integer progressPercent;
    @Column(name="progress_text", nullable=false, length=240)
    private String progressText;
    @Column(name="progress_image_asset_id")
    private UUID progressImageAssetId;
    @Column(name="customer_note", length=500)
    private String customerNote;
    @Column(name="internal_note", length=1200)
    private String internalNote;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;
    @Column(name="allow_public_case", nullable=false)
    private boolean allowPublicCase;
    @Column(name="public_summary", length=240)
    private String publicSummary;
    @Column(name="finished_at")
    private Instant finishedAt;
    @Column(name="public_enabled_at")
    private Instant publicEnabledAt;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Version
    private long version;

    @Generated
    public static CustomerOrderBuilder builder() {
        return new CustomerOrderBuilder();
    }

    @Generated
    public UUID getId() {
        return this.id;
    }

    @Generated
    public String getOrderNo() {
        return this.orderNo;
    }

    @Generated
    public String getBusinessNo() {
        return this.businessNo;
    }

    @Generated
    public UUID getSourceQuoteId() {
        return this.sourceQuoteId;
    }

    @Generated
    public Long getCustomerAccountId() {
        return this.customerAccountId;
    }

    @Generated
    public Status getStatus() {
        return this.status;
    }

    @Generated
    public Integer getProgressPercent() {
        return this.progressPercent;
    }

    @Generated
    public String getProgressText() {
        return this.progressText;
    }

    @Generated
    public UUID getProgressImageAssetId() {
        return this.progressImageAssetId;
    }

    @Generated
    public String getCustomerNote() {
        return this.customerNote;
    }

    @Generated
    public String getInternalNote() {
        return this.internalNote;
    }

    @Generated
    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public boolean isAllowPublicCase() {
        return this.allowPublicCase;
    }

    @Generated
    public String getPublicSummary() {
        return this.publicSummary;
    }

    @Generated
    public Instant getFinishedAt() {
        return this.finishedAt;
    }

    @Generated
    public Instant getPublicEnabledAt() {
        return this.publicEnabledAt;
    }

    @Generated
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public long getVersion() {
        return this.version;
    }

    @Generated
    public void setId(UUID id) {
        this.id = id;
    }

    @Generated
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    @Generated
    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    @Generated
    public void setSourceQuoteId(UUID sourceQuoteId) {
        this.sourceQuoteId = sourceQuoteId;
    }

    @Generated
    public void setCustomerAccountId(Long customerAccountId) {
        this.customerAccountId = customerAccountId;
    }

    @Generated
    public void setStatus(Status status) {
        this.status = status;
    }

    @Generated
    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    @Generated
    public void setProgressText(String progressText) {
        this.progressText = progressText;
    }

    @Generated
    public void setProgressImageAssetId(UUID progressImageAssetId) {
        this.progressImageAssetId = progressImageAssetId;
    }

    @Generated
    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    @Generated
    public void setInternalNote(String internalNote) {
        this.internalNote = internalNote;
    }

    @Generated
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public void setAllowPublicCase(boolean allowPublicCase) {
        this.allowPublicCase = allowPublicCase;
    }

    @Generated
    public void setPublicSummary(String publicSummary) {
        this.publicSummary = publicSummary;
    }

    @Generated
    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    @Generated
    public void setPublicEnabledAt(Instant publicEnabledAt) {
        this.publicEnabledAt = publicEnabledAt;
    }

    @Generated
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setVersion(long version) {
        this.version = version;
    }

    @Generated
    public CustomerOrder() {
    }

    @Generated
    public CustomerOrder(UUID id, String orderNo, String businessNo, UUID sourceQuoteId, Long customerAccountId, Status status, Integer progressPercent, String progressText, UUID progressImageAssetId, String customerNote, String internalNote, Instant updatedAt, boolean allowPublicCase, String publicSummary, Instant finishedAt, Instant publicEnabledAt, Instant createdAt, long version) {
        this.id = id;
        this.orderNo = orderNo;
        this.businessNo = businessNo;
        this.sourceQuoteId = sourceQuoteId;
        this.customerAccountId = customerAccountId;
        this.status = status;
        this.progressPercent = progressPercent;
        this.progressText = progressText;
        this.progressImageAssetId = progressImageAssetId;
        this.customerNote = customerNote;
        this.internalNote = internalNote;
        this.updatedAt = updatedAt;
        this.allowPublicCase = allowPublicCase;
        this.publicSummary = publicSummary;
        this.finishedAt = finishedAt;
        this.publicEnabledAt = publicEnabledAt;
        this.createdAt = createdAt;
        this.version = version;
    }
}

