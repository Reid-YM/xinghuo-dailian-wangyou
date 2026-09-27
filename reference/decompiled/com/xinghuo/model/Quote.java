/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.model.Quote
 *  com.xinghuo.model.Quote$QuoteBuilder
 *  com.xinghuo.model.Quote$Status
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  lombok.Generated
 */
package com.xinghuo.model;

import com.xinghuo.model.Quote;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Generated;

@Entity
@Table(name="quotes")
public class Quote {
    @Id
    private UUID id;
    @Column(name="business_no", nullable=false, unique=true, length=20)
    private String businessNo;
    @Column(nullable=false, length=100)
    private String game;
    @Column(nullable=false, length=200)
    private String demand;
    @Column(name="current_progress", length=500)
    private String currentProgress;
    @Column(name="screenshot_asset_id")
    private UUID screenshotAssetId;
    @Column(length=50)
    private String platform;
    @Column(name="expected_time", length=100)
    private String expectedTime;
    @Column(name="contact_method", length=40)
    private String contactMethod;
    @Column(name="customer_contact", length=120)
    private String customerContact;
    @Column(name="customer_account_id")
    private Long customerAccountId;
    @Enumerated(value=EnumType.STRING)
    @Column(nullable=false, length=24)
    private Status status;
    @Column(name="idempotency_key", nullable=false, unique=true, length=100)
    private String idempotencyKey;
    @Column(name="customer_ip_hash", length=128)
    private String customerIpHash;
    @Column(name="first_touch_source", length=64)
    private String firstTouchSource;
    @Column(name="utm_source", length=64)
    private String utmSource;
    @Column(name="utm_medium", length=64)
    private String utmMedium;
    @Column(name="utm_campaign", length=64)
    private String utmCampaign;
    @Column(name="referrer", length=300)
    private String referrer;
    @Column(name="lead_score")
    private Integer leadScore;
    @Column(name="lead_intent_level", length=20)
    private String leadIntentLevel;
    @Column(name="next_action", length=240)
    private String nextAction;
    @Column(name="follow_up_note", columnDefinition="TEXT")
    private String followUpNote;
    @Column(name="ai_suggestion", columnDefinition="TEXT")
    private String aiSuggestion;
    @Column(name="last_contacted_at")
    private Instant lastContactedAt;
    @Column(name="created_at", nullable=false)
    private Instant createdAt;
    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Generated
    public static QuoteBuilder builder() {
        return new QuoteBuilder();
    }

    @Generated
    public UUID getId() {
        return this.id;
    }

    @Generated
    public String getBusinessNo() {
        return this.businessNo;
    }

    @Generated
    public String getGame() {
        return this.game;
    }

    @Generated
    public String getDemand() {
        return this.demand;
    }

    @Generated
    public String getCurrentProgress() {
        return this.currentProgress;
    }

    @Generated
    public UUID getScreenshotAssetId() {
        return this.screenshotAssetId;
    }

    @Generated
    public String getPlatform() {
        return this.platform;
    }

    @Generated
    public String getExpectedTime() {
        return this.expectedTime;
    }

    @Generated
    public String getContactMethod() {
        return this.contactMethod;
    }

    @Generated
    public String getCustomerContact() {
        return this.customerContact;
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
    public String getIdempotencyKey() {
        return this.idempotencyKey;
    }

    @Generated
    public String getCustomerIpHash() {
        return this.customerIpHash;
    }

    @Generated
    public String getFirstTouchSource() {
        return this.firstTouchSource;
    }

    @Generated
    public String getUtmSource() {
        return this.utmSource;
    }

    @Generated
    public String getUtmMedium() {
        return this.utmMedium;
    }

    @Generated
    public String getUtmCampaign() {
        return this.utmCampaign;
    }

    @Generated
    public String getReferrer() {
        return this.referrer;
    }

    @Generated
    public Integer getLeadScore() {
        return this.leadScore;
    }

    @Generated
    public String getLeadIntentLevel() {
        return this.leadIntentLevel;
    }

    @Generated
    public String getNextAction() {
        return this.nextAction;
    }

    @Generated
    public String getFollowUpNote() {
        return this.followUpNote;
    }

    @Generated
    public String getAiSuggestion() {
        return this.aiSuggestion;
    }

    @Generated
    public Instant getLastContactedAt() {
        return this.lastContactedAt;
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
    public void setId(UUID id) {
        this.id = id;
    }

    @Generated
    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    @Generated
    public void setGame(String game) {
        this.game = game;
    }

    @Generated
    public void setDemand(String demand) {
        this.demand = demand;
    }

    @Generated
    public void setCurrentProgress(String currentProgress) {
        this.currentProgress = currentProgress;
    }

    @Generated
    public void setScreenshotAssetId(UUID screenshotAssetId) {
        this.screenshotAssetId = screenshotAssetId;
    }

    @Generated
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Generated
    public void setExpectedTime(String expectedTime) {
        this.expectedTime = expectedTime;
    }

    @Generated
    public void setContactMethod(String contactMethod) {
        this.contactMethod = contactMethod;
    }

    @Generated
    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
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
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    @Generated
    public void setCustomerIpHash(String customerIpHash) {
        this.customerIpHash = customerIpHash;
    }

    @Generated
    public void setFirstTouchSource(String firstTouchSource) {
        this.firstTouchSource = firstTouchSource;
    }

    @Generated
    public void setUtmSource(String utmSource) {
        this.utmSource = utmSource;
    }

    @Generated
    public void setUtmMedium(String utmMedium) {
        this.utmMedium = utmMedium;
    }

    @Generated
    public void setUtmCampaign(String utmCampaign) {
        this.utmCampaign = utmCampaign;
    }

    @Generated
    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    @Generated
    public void setLeadScore(Integer leadScore) {
        this.leadScore = leadScore;
    }

    @Generated
    public void setLeadIntentLevel(String leadIntentLevel) {
        this.leadIntentLevel = leadIntentLevel;
    }

    @Generated
    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    @Generated
    public void setFollowUpNote(String followUpNote) {
        this.followUpNote = followUpNote;
    }

    @Generated
    public void setAiSuggestion(String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }

    @Generated
    public void setLastContactedAt(Instant lastContactedAt) {
        this.lastContactedAt = lastContactedAt;
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
    public Quote() {
    }

    @Generated
    public Quote(UUID id, String businessNo, String game, String demand, String currentProgress, UUID screenshotAssetId, String platform, String expectedTime, String contactMethod, String customerContact, Long customerAccountId, Status status, String idempotencyKey, String customerIpHash, String firstTouchSource, String utmSource, String utmMedium, String utmCampaign, String referrer, Integer leadScore, String leadIntentLevel, String nextAction, String followUpNote, String aiSuggestion, Instant lastContactedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.businessNo = businessNo;
        this.game = game;
        this.demand = demand;
        this.currentProgress = currentProgress;
        this.screenshotAssetId = screenshotAssetId;
        this.platform = platform;
        this.expectedTime = expectedTime;
        this.contactMethod = contactMethod;
        this.customerContact = customerContact;
        this.customerAccountId = customerAccountId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.customerIpHash = customerIpHash;
        this.firstTouchSource = firstTouchSource;
        this.utmSource = utmSource;
        this.utmMedium = utmMedium;
        this.utmCampaign = utmCampaign;
        this.referrer = referrer;
        this.leadScore = leadScore;
        this.leadIntentLevel = leadIntentLevel;
        this.nextAction = nextAction;
        this.followUpNote = followUpNote;
        this.aiSuggestion = aiSuggestion;
        this.lastContactedAt = lastContactedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

